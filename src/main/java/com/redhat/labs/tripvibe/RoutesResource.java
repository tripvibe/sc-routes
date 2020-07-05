package com.redhat.labs.tripvibe;

import com.redhat.labs.tripvibe.models.*;
import com.redhat.labs.tripvibe.rest.*;
import com.redhat.labs.tripvibe.util.Signature;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status;

@Path("/api")
@ApplicationScoped
public class RoutesResource {

    private final Logger log = LoggerFactory.getLogger(RoutesResource.class);

    @ConfigProperty(name = "com.redhat.labs.tripvibe.developerId")
    public String devid;

    @ConfigProperty(name = "com.redhat.labs.tripvibe.isMock")
    public boolean isMock;

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
    @RestClient
    SearchService searchService;

    @Inject
    @RestClient
    SubmitQueryService submitQueryService;

    @Inject
    RemoteCacheManager cacheManager;

    RemoteCache<String, RouteType> routeTypeCache;
    RemoteCache<String, RouteNameNumber> routeNameNumberCache;
    RemoteCache<String, DirectionName> directionNameCache;

    @Inject
    @Remote("routesCache")
    RemoteCache<Integer, Route> routesCache;

    @Inject
    @Remote("directionsCache")
    RemoteCache<String, Direction> directionsCache;

    @Inject
    @Remote("vibeCache")
    RemoteCache<String, Double> vibeCache;

    @Inject
    @Remote("capacityCache")
    RemoteCache<String, Double> capacityCache;

    @Inject
    @Remote("stopsCache")
    RemoteCache<LatLongDistCacheKey, Stops> stopsCache;

    @Inject
    @Remote("searchCache")
    RemoteCache<String, Stops> searchCache;

    @Inject
    @Remote("tripVibeDAOCache")
    RemoteCache<RouteDirectionCacheKey, TripVibeDAO> tripVibeDAOCache;

    @Inject
    @Remote("departureDAOCache")
    RemoteCache<RouteDirectionCacheKey, DepartureDAO> departureDAOCache;

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        log.info("On start - get caches");
        routeTypeCache = cacheManager.administration().getOrCreateCache("routeType", DefaultTemplate.REPL_ASYNC);
        routeNameNumberCache = cacheManager.administration().getOrCreateCache("routeNameNumber", DefaultTemplate.REPL_ASYNC);
        directionNameCache = cacheManager.administration().getOrCreateCache("directionName", DefaultTemplate.REPL_ASYNC);
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

    private Multi<String> stopsMultiSearch(String route_types, String search_term) {
        return Multi.createFrom().item(searchService.routes(search_term, route_types, "false", devid, signature.generate("/v3/search/" + URLEncoder.encode(search_term, StandardCharsets.UTF_8).replace("+", "%20") + "?route_types=" + route_types + "&include_outlets=false"))).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Multi<List<RouteDAO>> departMultiSearch(String route_types, String search_term) {
        return Multi.createFrom().iterable(
                stopsMultiSearch(route_types, search_term).collectItems().asList().await().indefinitely()
        ).map(
                x -> _departures(new JSONObject(x))
        ).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/routes/{latlong}/{distance}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<List<RouteDAO>> stream(@PathParam String latlong, @PathParam String distance) {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(60)).onOverflow().drop();
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
    @Path("/routes/search/{route_types}/{search_term}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<List<RouteDAO>> streamSearch(@PathParam String route_types, @PathParam String search_term) {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(60)).onOverflow().drop();
        return ticks.on().subscribed(subscription -> log.info("We are subscribed!"))
                .on().cancellation(() -> log.info("Downstream has cancelled the interaction"))
                .onFailure().invoke(failure -> log.warn("Failed with " + failure.getMessage()))
                .onCompletion().invoke(() -> log.info("Completed"))
                .onItem().produceMulti(
                        x -> departMultiSearch(route_types, search_term)
                ).merge();
    }

    @GET
    @Path("/search/routes/search/{route_types}/{search_term}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RouteDAO> oneShotSearch(@PathParam String route_types, @PathParam String search_term) {
        return departMultiSearch(route_types, search_term).collectItems().first().await().indefinitely();
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
        cleanupCaches(routeTypeCache, routeNameNumberCache, directionNameCache, routesCache, directionsCache, vibeCache, capacityCache, stopsCache, searchCache, tripVibeDAOCache, departureDAOCache);
    }

    @GET
    @Path("/search/{route_types}/{search_term}")
    @Produces(MediaType.APPLICATION_JSON)
    public String search(@PathParam String search_term, @PathParam String route_types) {
        return searchService.routes(search_term, route_types, "false", devid, signature.generate("/v3/search/" + search_term + "?route_types=" + route_types + "&include_outlets=false"));
    }

    private List<RouteDAO> _departures(JSONObject obj) {

        List<RouteDAO> rList = new ArrayList<RouteDAO>();

        // get departures based on geoloc
        JSONArray stops = obj.getJSONArray("stops");

        // no results
        if (stops.length() == 0) {
            log.info("::_departures passed zero length stops returning");
            return rList;
        }

        Map<String, String> _sd = new ConcurrentHashMap<String, String>();
        Map<String, String> _sn = new ConcurrentHashMap<String, String>();
        for (int i = 0; i < stops.length(); i++) {
            JSONObject _stop = stops.getJSONObject(i);
            _sd.put(_stop.optString("stop_id"), _stop.optString("route_type"));
            _sn.put(_stop.optString("stop_id"), _stop.optString("stop_name"));
        }

        HashSet<DepartureCache> dhs = new HashSet<>();
        HashSet<RouteDirectionCacheKey> cks = new HashSet<>();

        log.info("RouteType Cache contains " + routeTypeCache.size() + " items ");
        log.info("RouteNameNumber Cache contains " + routeNameNumberCache.size() + " items ");
        log.info("DirectionName Cache contains " + directionNameCache.size() + " items ");

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
                        String scheduled_departure_utc = isMock ? getDepartureTimeMock() : _deps.optString("scheduled_departure_utc");

                        // remove duplicates
                        DepartureCache departureCache = new DepartureCache(route_type, stop_id, direction_id, scheduled_departure_utc);
                        if (dhs.contains(departureCache)) return;
                        dhs.add(departureCache);

                        try {
                            RouteDirectionCacheKey _key = new RouteDirectionCacheKey(k, v);
                            // remove duplicates
                            if (cks.contains(_key)) return;
                            cks.add(_key);

                            Double capacity = isMock ? getCapacityMock() : capacityAverage(route_id);
                            Double vibe = isMock ? getVibeMock() : vibeAverage(route_id);
                            String routeName = routeNameNumber(route_id, "route_name");
                            String routeNumber = routeNameNumber(route_id, "route_number");
                            String routeDirection = directionName(route_id, direction_id);
                            RouteDAO _r = new RouteDAO(routeTypes(route_type), routeName, routeNumber, routeDirection, _sn.get(k), capacity, vibe, scheduled_departure_utc, direction_id, route_id);
                            log.debug("RouteDAO: " + _r);
                            rList.add(_r);

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
        if (routeTypeCache.containsKey(route_type)) {
            return routeTypeCache.get(route_type).getRoute_type_name();
        }
        // Route Type Service Call
        String routeTypes = routeTypeService.routes(devid, signature.generate("/v3/route_types"));
        JSONObject r = new JSONObject(routeTypes);
        JSONArray rts = r.getJSONArray("route_types");
        Map<String, String> _rt = new ConcurrentHashMap<String, String>();
        for (int i = 0; i < rts.length(); i++) {
            JSONObject _rts = rts.getJSONObject(i);
            _rt.put(_rts.optString("route_type"), _rts.optString("route_type_name"));
        }
        String routeTypeName = _rt.get(route_type);
        routeTypeCache.put(route_type, new RouteType(route_type, routeTypeName), 3600 * 24, TimeUnit.SECONDS);
        return routeTypeName;
    }

    private String routeNameNumber(String route_id, String nn) {
        if (routeNameNumberCache.containsKey(route_id)) {
            if (nn.equalsIgnoreCase("route_name"))
                return routeNameNumberCache.get(route_id).getRoute_name();
            else
                return routeNameNumberCache.get(route_id).getRoute_number();
        }
        // Route Name Service Call
        String routeName = routeService.route(route_id, devid, signature.generate("/v3/routes/" + route_id));
        JSONObject r = new JSONObject(routeName);
        String rn = r.getJSONObject("route").getString("route_name");
        log.debug("routeNameNumber " + r);
        String rnn = r.getJSONObject("route").getString("route_number");
        try {
            RouteNameNumber rnnObj = new RouteNameNumber(rn, rnn);
            routeNameNumberCache.put(route_id, rnnObj, 3600 * 12, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("Can't cache routeNameNumber '" + rnn + "'. Reason: " + ex.getMessage());
        }
        if (nn.equalsIgnoreCase("route_name"))
            return rn;
        return rnn;
    }

    private String directionName(String route_id, String direction_id) {
        if (directionNameCache.containsKey(direction_id)) {
            return directionNameCache.get(direction_id).getDirection_name();
        }
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
        String dn = _rt.get(direction_id);
        directionNameCache.put(direction_id, new DirectionName(direction_id, dn), 3600 * 12, TimeUnit.SECONDS);
        return dn;
    }

    private void cleanupCaches(RemoteCache<String, RouteType> routeType,
                               RemoteCache<String, RouteNameNumber> routeNameNumber,
                               RemoteCache<String, DirectionName> directionName,
                               RemoteCache<Integer, Route> routesCache,
                               RemoteCache<String, Direction> directionsCache,
                               RemoteCache<String, Double> vibeCache,
                               RemoteCache<String, Double> capacityCache,
                               RemoteCache<LatLongDistCacheKey, Stops> stopsCache,
                               RemoteCache<String, Stops> searchCache,
                               RemoteCache<RouteDirectionCacheKey, TripVibeDAO> tripVibeDAOCache,
                               RemoteCache<RouteDirectionCacheKey, DepartureDAO> departureDAOCache) {
        try {
            Uni.createFrom().item(routeType.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(routeNameNumber.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(directionName.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(routesCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(directionsCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(vibeCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(capacityCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(stopsCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(searchCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(tripVibeDAOCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
            Uni.createFrom().item(departureDAOCache.clearAsync().get(10, TimeUnit.SECONDS))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).await().indefinitely();
        } catch (Exception e) {
            log.error("Something went wrong clearing data stores." + e);
        }
    }

    private Double capacityAverage(String route_id) {
        Double cap = -1.0;
        try {
            Double ret = submitQueryService.capacityAverage(route_id);
            if (null != ret) cap = ret;
        } catch (javax.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == Status.NOT_FOUND.getStatusCode()
                    || e.getResponse().getStatus() == Status.NO_CONTENT.getStatusCode()) {
                // OK nothing collected yet, return default
                log.debug("capacityAverage - nothing found: " + e.getResponse().getStatus());
            } else {
                log.error("capacityAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        return cap;
    }

    private Double vibeAverage(String route_id) {
        Double vib = -1.0;
        try {
            Double ret = submitQueryService.vibeAverage(route_id);
            if (null != ret) vib = ret;
        } catch (javax.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == Status.NOT_FOUND.getStatusCode()
                    || e.getResponse().getStatus() == Status.NO_CONTENT.getStatusCode()) {
                // OK nothing collected yet, return default
                log.debug("vibeAverage - nothing found: " + e.getResponse().getStatus());
            } else {
                log.error("vibeAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        return vib;
    }

    /*
      Mocks
     */
    private Double getCapacityMock() {
        Integer i = new Random().nextInt(100) + 1;
        return i.doubleValue();
    }

    private Double getVibeMock() {
        Integer i = new Random().nextInt(100) + 1;
        return i.doubleValue();
    }

    private String getDepartureTimeMock() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        long now = new Date().getTime();
        long minutes = (new Random().nextInt(10) + 1) * 60000;
        Date date = new Date(now + minutes);
        return df.format(date);
    }

}
