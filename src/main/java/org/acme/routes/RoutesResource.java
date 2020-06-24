package org.acme.routes;

import com.acme.dao.RouteDAO;
import com.acme.rest.*;
import com.acme.util.Signature;
import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jboss.resteasy.annotations.SseElementType;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Path("/api")
@ApplicationScoped
public class RoutesResource {

    private final Logger log = LoggerFactory.getLogger(RoutesResource.class);

    @ConfigProperty(name = "com.acme.developerId")
    public String devid;

    @Inject
    Signature signature;

    @Inject
    @RestClient
    StopsService stopsService;

    @Inject
    @RestClient
    DeparturesService departuresService;

    @Inject
    @RestClient
    RouteService routeService;

    @Inject
    @RestClient
    RouteTypeService routeTypeService;

    @Inject
    @RestClient
    DirectionService directionService;

    @Inject
    RemoteCacheManager cacheManager;

    @Inject
    @Remote("routes")
    RemoteCache<Integer, RouteDAO> routesCache;

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        log.info("On start - clean and load data");
        RemoteCache<Integer, RouteDAO> routes = cacheManager.administration().getOrCreateCache("routes", DefaultTemplate.REPL_ASYNC);
        log.info("Existing stores are " + cacheManager.getCacheNames().toString());
    }

    private Multi<String> stopsMulti(String latlong, String distance) {
        return Multi.createFrom().item(stopsService.routes(latlong, distance, devid, signature.generate("/v3/stops/location/" + latlong + "?max_distance=" + distance))).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Multi<List<RouteDAO>> departMulti(String latlong, String distance) {
        return Multi.createFrom().iterable(
                stopsMulti(latlong, distance).collectItems().asList().await().indefinitely()
        ).map(
                x -> _departures(new JSONObject(x))
        ).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/routes/{latlong}/{distance}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<List<RouteDAO>> stream(@PathParam String latlong, @PathParam String distance) {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(10)).onOverflow().drop();
        return ticks.on().subscribed(subscription -> log.info("We are subscribed!"))
                .on().cancellation(() -> log.info("Downstream has cancelled the interaction"))
                .onFailure().invoke(failure -> log.warn("Failed with " + failure.getMessage()))
                .onCompletion().invoke(() -> log.info("Completed"))
                .onItem().produceMulti(
                        x -> departMulti(latlong, distance)
                ).merge();
    }

    @GET
    @Path("/search/routes/{latlong}/{distance}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RouteDAO> oneShot(@PathParam String latlong, @PathParam String distance) {
        return departMulti(latlong, distance).collectItems().first().await().indefinitely();
    }

    @GET
    @Path("/stops/{latlong}/{distance}")
    @Produces(MediaType.APPLICATION_JSON)
    public String stops(@PathParam String latlong, @PathParam String distance) {
        return stopsService.routes(latlong, distance, devid, signature.generate("/v3/stops/location/" + latlong + "?max_distance=" + distance));
    }

    @GET
    @Path("/departures/{route_type}/{stop_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String departures(@PathParam String route_type, @PathParam String stop_id) {
        return departuresService.departures(route_type, stop_id, devid, signature.generate("/v3/departures/route_type/" + route_type + "/stop/" + stop_id));
    }

    @GET
    @Path("/route/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String route(@PathParam String route_id) {
        return routeService.route(route_id, devid, signature.generate("/v3/routes/" + route_id));
    }

    @GET
    @Path("/route_types")
    @Produces(MediaType.APPLICATION_JSON)
    public String routeType(@PathParam String route_id) {
        return routeTypeService.routes(devid, signature.generate("/v3/route_types"));
    }

    @GET
    @Path("/directions/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String direction(@PathParam String route_id) {
        return directionService.directions(route_id, devid, signature.generate("/v3/directions/route/" + route_id));
    }

    @DELETE
    @Path("/clearcache")
    public void cleanCache() {
        cleanupCaches(routesCache);
    }

    private List<RouteDAO> _departures(JSONObject obj) {
        // get departures based on geoloc
        JSONArray stops = obj.getJSONArray("stops");

        // no results
        if (stops.length() == 0) {
            log.info("::_departures passed zero length stops returning");
            return new ArrayList();
        }

        Map<String, String> _sd = new ConcurrentHashMap<String, String>();
        Map<String, String> _sn = new ConcurrentHashMap<String, String>();
        for (int i = 0; i < stops.length(); i++) {
            JSONObject _stop = stops.getJSONObject(i);
            _sd.put(_stop.optString("stop_id"), _stop.optString("route_type"));
            _sn.put(_stop.optString("stop_id"), _stop.optString("stop_name"));
        }

        List<RouteDAO> rList = new ArrayList<RouteDAO>();
        Map<String, String> duplicates = new ConcurrentHashMap<String, String>();

        log.info("Cache contains " + routesCache.size() + " items ");

        _sd.forEach((k, v) -> {
                    final String route_type = v;
                    final String stop_id = k;
                    // Service call for departures
                    String departures = departuresService.departures(route_type, stop_id, devid, signature.generate("/v3/departures/route_type/" + route_type + "/stop/" + stop_id));

                    // we only want unique route and direction
                    JSONObject d = new JSONObject(departures);
                    JSONArray deps = d.getJSONArray("departures");
                    Map<String, String> _rd = new ConcurrentHashMap<String, String>();
                    for (int i = 0; i < deps.length(); i++) {
                        JSONObject _deps = deps.getJSONObject(i);
                        _rd.put(_deps.optString("route_id"), _deps.optString("direction_id"));
                    }
                    log.debug("::_departures found " + _rd.size() + " processing...");

                    // RouteType
                    final String rT = routeTypes(route_type);

                    // Populate return list using cache if it exists
                    _rd.forEach((key, val) -> {
                        try {
                            JSONObject ret = new JSONObject();
                            if (routesCache.containsKey(Integer.valueOf(key))) {
                                log.debug("Reading " + key + " from cache...");
                                RouteDAO routeDAO = routesCache.get(Integer.valueOf(key));
                                String routeName = routeDAO.getName();
                                String routeNumber = routeDAO.getNumber();
                                if (!duplicates.containsKey(routeName)) {
                                    ret.put("Type", routeDAO.getType());
                                    ret.put("Name", routeDAO.getName());
                                    ret.put("Number", routeDAO.getNumber());
                                    ret.put("Direction", routeDAO.getDirection());
                                    ret.put("StopName", routeDAO.getStopName());
                                    ret.put("Capacity", routeDAO.getCapacity());
                                    ret.put("Vibe", routeDAO.getVibe());
                                    ret.put("DepartureTime", routeDAO.getDepartureTime());
                                    rList.add(routeDAO);
                                }
                                duplicates.put(routeName, routeNumber);
                            } else {
                                String routeName = routeNameNumber(key, "route_name");
                                String routeNumber = routeNameNumber(key, "route_number");
                                Integer capacity = getCapcaity();
                                Integer vibe = getVibe();
                                String departureTime = getDepartureTime();
                                if (!duplicates.containsKey(routeName)) {
                                    String routeDirection = directionName(key, val);
                                    ret.put("Type", rT);
                                    ret.put("Name", routeName);
                                    ret.put("Number", routeNumber);
                                    ret.put("Direction", routeDirection);
                                    ret.put("StopName", _sn.get(k));
                                    ret.put("Capacity", capacity);
                                    ret.put("Vibe", vibe);
                                    ret.put("DepartureTime", departureTime);
                                    RouteDAO _r = new RouteDAO(rT, routeName, routeNumber, routeDirection, _sn.get(k), capacity, vibe, departureTime);
                                    rList.add(_r);
                                    //routesCache.put(Integer.valueOf(key), _r, 300, TimeUnit.SECONDS);
                                }
                                duplicates.put(routeName, routeNumber);
                            }
                        } catch (org.json.JSONException ex) {
                            log.error("JSON Parse error." + ex);
                        }
                    });
                }
        );

        log.info("Found " + rList.size() + " departure routes nearby ...");
        return rList;
    }

    private String routeTypes(String route_type) {
        // Route Type Service Call
        String routeTypes = routeTypeService.routes(devid, signature.generate("/v3/route_types"));
        JSONObject r = new JSONObject(routeTypes);
        JSONArray rts = r.getJSONArray("route_types");
        Map<String, String> _rt = new ConcurrentHashMap<String, String>();
        for (int i = 0; i < rts.length(); i++) {
            JSONObject _rts = rts.getJSONObject(i);
            _rt.put(_rts.optString("route_type"), _rts.optString("route_type_name"));
        }
        return _rt.get(route_type);
    }

    private String routeNameNumber(String route_id, String nn) {
        // Route Name Service Call
        String routeName = routeService.route(route_id, devid, signature.generate("/v3/routes/" + route_id));
        JSONObject r = new JSONObject(routeName);
        String rn = r.getJSONObject("route").getString("route_name");
        log.debug("routeNameNumber " + r);
        String rnn = r.getJSONObject("route").getString("route_number");
        if (nn.equalsIgnoreCase("route_name"))
            return rn;
        return rnn;
    }

    private String directionName(String route_id, String direction_id) {
        // Direction Name Service Call
        String directionName = directionService.directions(route_id, devid, signature.generate("/v3/directions/route/" + route_id));
        JSONObject r = new JSONObject(directionName);
        JSONArray rts = r.getJSONArray("directions");
        log.debug("directionName " + r);
        Map<String, String> _rt = new ConcurrentHashMap<String, String>();
        for (int i = 0; i < rts.length(); i++) {
            JSONObject _rts = rts.getJSONObject(i);
            _rt.put(_rts.optString("direction_id"), _rts.optString("direction_name"));
        }
        return _rt.get(direction_id);
    }

    private void cleanupCaches(RemoteCache<Integer, RouteDAO> routes) {
        try {
            Uni.createFrom().item(routes.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
        } catch (Exception e) {
            log.error("Something went wrong clearing data stores." + e);
        }
    }


    /*
      The methods are FAKE/MOCK for now
     */
    private Integer getCapcaity() {
        return new Random().nextInt(100) + 1;
    }

    private Integer getVibe() {
        return new Random().nextInt(100) + 1;
    }

    private String getDepartureTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        return df.format(date);
    }

}
