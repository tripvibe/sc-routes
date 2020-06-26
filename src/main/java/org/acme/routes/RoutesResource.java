package org.acme.routes;

import com.acme.dao.CacheKey;
import com.acme.dao.Departure;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
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
    RemoteCache<CacheKey, RouteDAO> routesCache;

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        log.info("On start - clean and load data");
        RemoteCache<CacheKey, RouteDAO> routes = cacheManager.administration().getOrCreateCache("routes", DefaultTemplate.REPL_ASYNC);
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
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(20)).onOverflow().drop();
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
        HashSet<Departure> dhs = new HashSet<>();
        HashSet<CacheKey> cks = new HashSet<>();

        log.info("Cache contains " + routesCache.size() + " items ");

        _sd.forEach((k, v) -> {
                    final String route_type = v;
                    final String stop_id = k;

                    // Service call for departures
                    String departures = departuresService.departures(route_type, stop_id, devid, signature.generate("/v3/departures/route_type/" + route_type + "/stop/" + stop_id));

                    JSONObject d = new JSONObject(departures);
                    JSONArray deps = d.getJSONArray("departures");

                    log.debug("::_departures found " + deps.length() + " processing...");

                    for (int i = 0; i < deps.length(); i++) {
                        JSONObject _deps = deps.getJSONObject(i);
                        String route_id = _deps.optString("route_id");
                        String direction_id = _deps.optString("direction_id");
                        String scheduled_departure_utc = _deps.optString("scheduled_departure_utc");

                        // remove duplicates
                        Departure departure = new Departure(route_type, stop_id, direction_id, scheduled_departure_utc);
                        if (dhs.contains(departure)) return;
                        dhs.add(departure);

                        try {
                            CacheKey _key = new CacheKey(k, v);
                            // remove duplicates
                            if (cks.contains(_key)) return;
                            cks.add(_key);

                            if (routesCache.containsKey(_key)) {
                                log.debug("Reading " + _key + " from cache...");
                                RouteDAO routeDAO = routesCache.get(_key);
                                rList.add(routeDAO);
                            } else {
                                Integer capacity = getCapcaity();
                                Integer vibe = getVibe();
                                String routeName = routeNameNumber(route_id, "route_name");
                                String routeNumber = routeNameNumber(route_id, "route_number");
                                String routeDirection = directionName(route_id, direction_id);
                                RouteDAO _r = new RouteDAO(routeTypes(route_type), routeName, routeNumber, routeDirection, _sn.get(k), capacity, vibe, scheduled_departure_utc);
                                rList.add(_r);
                                routesCache.put(_key, _r); // , 3600, TimeUnit.SECONDS
                            }
                        } catch (org.json.JSONException ex) {
                            log.error("JSON Parse error." + ex);
                        }
                    }
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

    private void cleanupCaches(RemoteCache<CacheKey, RouteDAO> routes) {
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
        long now = new Date().getTime();
        long minutes = (new Random().nextInt(10) + 1) * 60000;
        Date date = new Date(now + minutes);
        return df.format(date);
    }

}
