package com.redhat.labs.tripvibe;

import com.redhat.labs.tripvibe.models.*;
import com.redhat.labs.tripvibe.rest.SubmitQueryService;
import com.redhat.labs.tripvibe.services.*;
import com.redhat.labs.tripvibe.util.Signature;
import io.quarkus.runtime.StartupEvent;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/api")
@ApplicationScoped
public class DepartureResource {

    private final Logger log = LoggerFactory.getLogger(DepartureResource.class);

    @ConfigProperty(name = "com.redhat.labs.tripvibe.developerId")
    public String devid;

    public static Map<Integer, Route> localRoutesCache = new HashMap<>();
    public static Map<Integer, Instant> localRoutesCacheAge = new HashMap<>();
    public static Map<String, Direction> localDirectionsCache = new HashMap<>();
    public static Map<String, Instant> localDirectionsCacheAge = new HashMap<>();
    private Integer maxCacheAgeHour = 24; //keep the cached objects upto 24 hours
    // use to retrieve departures for the next 2 hours
    private int nextSeconds = 3600 * 2;
    private int pastSeconds = 0;

    @Inject
    @RestClient
    StopRestService stopsService;

    @Inject
    @RestClient
    DepartureRestService departureService;

    @Inject
    @RestClient
    RouteRestService routeService;

    @Inject
    @RestClient
    DirectionRestService directionService;

    @Inject
    @RestClient
    SearchRestService searchService;

    @Inject
    @RestClient
    SubmitQueryService submitQueryService;

    @Inject
    Signature signature;

    @Inject
    RemoteCacheManager cacheManager;

    @ConfigProperty(name = "com.redhat.labs.tripvibe.enableCache")
    Boolean enableCache = false;

    RemoteCache<Integer, Route> routesCache;
    RemoteCache<String, Direction> directionsCache;
    RemoteCache<String, Double> vibeCache;
    RemoteCache<String, Double> capacityCache;
    RemoteCache<String, Stops> stopsCache;
    RemoteCache<String, Stops> searchCache;
    RemoteCache<String, TripVibeDAO> tripVibeDAOCache;
    RemoteCache<String, DepartureDAO> departureDAOCache;

    private static final String CACHE_CONFIG = "<infinispan><cache-container>" +
            "<distributed-cache name=\"%s\" mode=\"ASYNC\"></distributed-cache>" +
            "</cache-container></infinispan>";

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        if (!enableCache) return;
        log.info("On start - get caches");
        routesCache = cacheManager.administration().getOrCreateCache("routesCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "routesCache")));
        directionsCache = cacheManager.administration().getOrCreateCache("directionsCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "directionsCache")));
        vibeCache = cacheManager.administration().getOrCreateCache("vibeCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "vibeCache")));
        capacityCache = cacheManager.administration().getOrCreateCache("capacityCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "capacityCache")));
        stopsCache = cacheManager.administration().getOrCreateCache("stopsCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "stopsCache")));
        searchCache = cacheManager.administration().getOrCreateCache("searchCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "searchCache")));
        tripVibeDAOCache = cacheManager.administration().getOrCreateCache("tripVibeDAOCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "tripVibeDAOCache")));
        departureDAOCache = cacheManager.administration().getOrCreateCache("departureDAOCache", new XMLStringConfiguration(String.format(CACHE_CONFIG, "departureDAOCache")));
        log.info("Existing stores are " + cacheManager.getCacheNames().toString());
    }

    @GET
    @Path("/evict-single/{cacheKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "evictSingle",
            summary = "evict a single sentiment",
            description = "This operation allows you to evict a sentiment from the cache based on route_id\",",
            deprecated = false,
            hidden = false)
    public void evictSingle(@PathParam String cacheKey) {
        log.info("Evicting for: " + cacheKey);
        vibeCache.remove(cacheKey);
        capacityCache.remove(cacheKey);
        tripVibeDAOCache.remove(cacheKey);
    }

    private Set<Departure> getDepartures(Stop stop) {
        Uni<DepartureResponse> mdr = Uni.createFrom().item(departureService.departures(stop.getRoute_type(), stop.getStop_id(),
                devid, signature.generate("/v3/departures/route_type/" + stop.getRoute_type() + "/stop/" + stop.getStop_id()))).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        return mdr.await().indefinitely().getDepartures();
    }

    @GET
    @Path("/nearby-departures/{latlong}/{distance}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "getNearbyDepartures",
            summary = "get nearby departures",
            description = "This operation allows you to get all nearby departures based on geoloc and distance\",",
            deprecated = false,
            hidden = false)
    public Set<TripVibeDAO> getNearbyDepartures(@PathParam String latlong, @PathParam String distance, @DefaultValue("0") @QueryParam Integer pastSeconds, @QueryParam Integer nextSeconds) {

        if (nextSeconds != null) this.nextSeconds = nextSeconds;
        if (pastSeconds != null) this.pastSeconds = pastSeconds;

        log.info("Retrieving departures by stop using latlong: " + latlong + " distance: " + distance + " with pastSeconds: " + this.pastSeconds + " nextSeconds: " + this.nextSeconds);

        String lldkey = String.format("%s-%s-%s-%s", latlong, distance, this.pastSeconds, this.nextSeconds);
        Stops stops = new Stops();
        if (enableCache && stopsCache.containsKey(lldkey)) {
            stops = stopsCache.get(lldkey);
        } else {
            Set<Stop> st = stopsService.stops(latlong, distance, devid, signature.generate("/v3/stops/location/" + latlong + "?max_distance=" + distance)).getStops();
            stops.setStops(st);
            if (stops.getStops().size() == 0) {
                return new HashSet<>(); // No stops nearby, return immediately
            } else {
                stopsCache.put(lldkey, stops, 2, TimeUnit.HOURS);
            }
        }
        log.info("Stops count : " + stops.getStops().size());
        if (enableCache) {
            printCacheSizes();
        }

        HashSet<TripVibeDAO> nearbyDepartures = new HashSet<TripVibeDAO>();

        // parallel calls for destinations per stop
        final List<Single<Set<TripVibeDAO>>> listOfSingles = Collections.synchronizedList(new ArrayList<>());
        stops.getStops().forEach(
                stop -> {
                    final Single<Set<TripVibeDAO>> responseSingle = Single.fromCallable(() -> {
                        return _tripvibeDAO(stop);
                    }).subscribeOn(Schedulers.io());
                    listOfSingles.add(responseSingle);
                }
        );
        Flowable<Set<TripVibeDAO>> last = Single.merge(listOfSingles);
        last.blockingForEach(s -> nearbyDepartures.addAll(s));

        log.info("Nearby departures returning count: " + nearbyDepartures.size());
        return nearbyDepartures;
    }

    // this will not change in a decade
    private String getRouteTypeName(int type) {
        switch (type) {
            case 0:
                return "Train";
            case 1:
                return "Tram";
            case 2:
                return "Bus";
            case 3:
                return "VLine";
            case 4:
                return "Night Bus";
            default:
                return "Unknown";
        }
    }

    private Route getRouteById(int routeId) {
        if (!enableCache) {
            if (localRoutesCache.containsKey(routeId) && localRoutesCacheAge.containsKey(routeId)
                    && localRoutesCacheAge.get(routeId).isBefore(Instant.now().plus(maxCacheAgeHour, ChronoUnit.HOURS))) {
                return localRoutesCache.get(routeId);
            }
            RouteResponse route = routeService.route(routeId, devid, signature.generate("/v3/routes/" + routeId));
            localRoutesCache.put(routeId, route.route);
            localRoutesCacheAge.put(routeId, Instant.now());
            return route.route;
        }

        if (routesCache.containsKey(routeId)) {
            return routesCache.get(routeId);
        }

        RouteResponse route = routeService.route(routeId, devid, signature.generate("/v3/routes/" + routeId));
        routesCache.put(routeId, route.route, 3600L * 12L, TimeUnit.SECONDS);
        return route.route;
    }

    private Direction getDirectionById(int directionId, int routeId, int routeType) {
        String cacheKey = String.format("%s-%s-%s", directionId, routeId, routeType);
        log.debug("::getDirectionById " + cacheKey);
        if (!enableCache) {
            if (localDirectionsCache.containsKey(cacheKey) && localDirectionsCacheAge.containsKey(cacheKey)
                    && localDirectionsCacheAge.get(cacheKey).isBefore(Instant.now().plus(maxCacheAgeHour, ChronoUnit.HOURS))) {
                return localDirectionsCache.get(cacheKey);
            }

            Direction direction = directionService.directions(routeId, devid, signature.generate("/v3/directions/route/" + routeId))
                    .getDirections().stream().filter(d -> d.getDirection_id() == directionId && d.getRoute_type() == routeType)
                    .findFirst()
                    .orElse(null);
            localDirectionsCache.put(cacheKey, direction);
            localDirectionsCacheAge.put(cacheKey, Instant.now());
            return direction;
        }

        if (directionsCache.containsKey((cacheKey))) {
            return directionsCache.get(cacheKey);
        }
        Set<Direction> directions = directionService.directions(routeId, devid, signature.generate("/v3/directions/route/" + routeId))
                .getDirections();
        if (directions.isEmpty()) {
            return null;
        }
        Direction direction = directions.stream()
                .filter(d -> d.getDirection_id() == directionId && d.getRoute_type() == routeType)
                .findFirst()
                .orElse(null);

        directionsCache.put(cacheKey, direction);
        return direction;
    }

    @GET
    @Path("/search-departures/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "searchDepartures",
            summary = "get nearby departures by search_term",
            description = "This operation allows you to get all nearby departures based on search_term\",",
            deprecated = false,
            hidden = false)
    public Set<DepartureDAO> searchDepartures(@PathParam String term, @QueryParam int routeType,
                                              @DefaultValue("0") @QueryParam Integer pastSeconds, @QueryParam Integer nextSeconds) {

        if (nextSeconds != null) this.nextSeconds = nextSeconds;
        if (pastSeconds != null) this.pastSeconds = pastSeconds;

        log.info("Retrieving departures by stop using keyword: " + term + " with route_type: " + routeType + " and pastSeconds: " + this.pastSeconds + " nextSeconds: " + this.nextSeconds);

        Stops stops = new Stops();
        String ckey = String.format("%s-%s-%s-%s", term, this.pastSeconds, this.nextSeconds, routeType);
        if (enableCache && searchCache.containsKey(ckey)) {
            stops = searchCache.get(ckey);
        } else {
            Set<Stop> st = searchService.search(term, routeType, devid,
                    signature.generate("/v3/search/" + term.replace(" ", "%20") + "?route_types=" + routeType)).getStops();
            stops.setStops(st);
        }
        log.info("Stops count : " + stops.getStops().size());
        if (stops.getStops().size() == 0) {
            return new HashSet<>(); // No stops nearby, return immediately
        }

        if (enableCache) {
            searchCache.put(ckey, stops, 2, TimeUnit.HOURS);
            printCacheSizes();
        }

        HashSet<DepartureDAO> nearbyDepartures = new HashSet<DepartureDAO>();

        // parallel calls for destinations per stop
        final List<Single<Set<DepartureDAO>>> listOfSingles = Collections.synchronizedList(new ArrayList<>());
        stops.getStops().forEach(
                stop -> {
                    final Single<Set<DepartureDAO>> responseSingle = Single.fromCallable(() -> {
                        return _departuresDAO(stop);
                    }).subscribeOn(Schedulers.io());
                    listOfSingles.add(responseSingle);
                }
        );
        Flowable<Set<DepartureDAO>> last = Single.merge(listOfSingles);
        last.blockingForEach(s -> nearbyDepartures.addAll(s));

        log.info("Nearby departures returning count: " + nearbyDepartures.size());
        return nearbyDepartures;
    }

    /*
        average vibe + capacity algorithm
        (1) try by trip (route_id, route_type, direction_id, run_id, stop_id)
        (2) then by (route_id)

        r1 - keyed by route_id, rolling 1min avg
        r5 - keyed by route_id, rolling 5min avg
        t1 - keyed by trip ids, rolling 1min avg
        t5 - keyed by trip ids, rolling 5min avg
    */
    private Double capacityAverage(String route_id, String route_type, String direction_id, String run_id, String
            stop_id) {
        Double cap = -1.0;
        String cacheKey = String.format("%s-%s-%s-%s-%s-%s", "t1", route_id, route_type, direction_id, run_id, stop_id);
        if (enableCache && capacityCache.containsKey(cacheKey)) {
            cap = capacityCache.get(cacheKey);
            log.debug("capacityAverage " + cacheKey + " " + cap);
            return cap;
        }
        cacheKey = String.format("%s-%s", "r1", route_id);
        if (enableCache && cap == -1.0 && capacityCache.containsKey(cacheKey)) {
            cap = capacityCache.get(cacheKey);
        }
        log.debug("capacityAverage " + cacheKey + " " + cap);
        return cap;
    }

    private Double vibeAverage(String route_id, String route_type, String direction_id, String run_id, String
            stop_id) {
        Double vib = -1.0;
        String cacheKey = String.format("%s-%s-%s-%s-%s-%s", "t1", route_id, route_type, direction_id, run_id, stop_id);
        if (enableCache && vibeCache.containsKey(cacheKey)) {
            vib = vibeCache.get(cacheKey);
            log.debug("vibeAverage " + cacheKey + " " + vib);
            return vib;
        }
        cacheKey = String.format("%s-%s", "r1", route_id);
        if (enableCache && vib == -1.0 && vibeCache.containsKey(cacheKey)) {
            vib = vibeCache.get(cacheKey);
        }
        log.debug("vibeAverage " + cacheKey + " " + vib);
        return vib;
    }

    private void printCacheSizes() {
        log.info("Routes Cache contains " + routesCache.size() + " items ");
        log.info("Directions Cache contains " + directionsCache.size() + " items ");
        log.info("Vibe Cache contains " + vibeCache.size() + " items ");
        log.info("Capacity Cache contains " + capacityCache.size() + " items ");
        log.info("Stops Cache contains " + stopsCache.size() + " items ");
        log.info("Search Cache contains " + searchCache.size() + " items ");
        log.info("TripVibeDAO Cache contains " + tripVibeDAOCache.size() + " items ");
        log.info("DepartureDAO Cache contains " + departureDAOCache.size() + " items ");
    }

    private <T> Set<T> convertListToSet(List<T> list) {
        // create a set from the List
        return list.stream().collect(Collectors.toSet());
    }

    private Set<TripVibeDAO> _tripvibeDAO(Stop stop) {
        Instant utcNow = Instant.now();
        List<Departure> depList = Multi.createFrom().iterable(getDepartures(stop)).transform()
                .byFilteringItemsWith(
                        dep -> dep.getScheduled_departure_utc().isAfter(utcNow.minus(this.pastSeconds, ChronoUnit.SECONDS))
                                && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextSeconds, ChronoUnit.SECONDS)))
                .collectItems().asList().await().indefinitely();
        Set<Departure> departures = convertListToSet(depList);
        log.debug("Departures count : " + departures.size());

        Set<TripVibeDAO> nearby = new HashSet<TripVibeDAO>();
        if (!departures.isEmpty()) {
            nearby = departures.stream().map(dep -> {
                String cacheKey = String.format("%s-%s-%s-%s-%s-%s", dep.getRoute_id().toString(), stop.getRoute_type(), dep.getDirection_id().toString(), dep.getRun_id(), dep.getStop_id(), dep.getScheduled_departure_utc().toString());
                if (enableCache && tripVibeDAOCache.containsKey(cacheKey)) {
                    return tripVibeDAOCache.get(cacheKey);
                }
                Route route = getRouteById(dep.getRoute_id());
                Direction direction = getDirectionById(dep.getDirection_id(), dep.getRoute_id(), stop.getRoute_type());
                if (null == route || null == direction) {
                    return null;
                }
                TripVibeDAO t = new TripVibeDAO(
                        route.getRoute_name(),
                        route.getRoute_number(),
                        direction.getDirection_name(),
                        stop.getStop_name(),
                        dep.getScheduled_departure_utc().toString(),
                        getRouteTypeName(stop.getRoute_type()),
                        dep.getAt_platform(),
                        dep.getEstimated_departure_utc() == null ? null : dep.getEstimated_departure_utc().toString(),
                        dep.getPlatform_number() == null ? null : dep.getPlatform_number().toString(),
                        dep.getRoute_id(),
                        dep.getStop_id(),
                        dep.getRun_id(),
                        dep.getDirection_id(),
                        capacityAverage(dep.getRoute_id().toString(), stop.getRoute_type().toString(), dep.getDirection_id().toString(), dep.getRun_id().toString(), dep.getStop_id().toString()),
                        vibeAverage(dep.getRoute_id().toString(), stop.getRoute_type().toString(), dep.getDirection_id().toString(), dep.getRun_id().toString(), dep.getStop_id().toString()));
                if (enableCache) {
                    tripVibeDAOCache.put(cacheKey, t, 60, TimeUnit.SECONDS);
                }
                return t;
            }).filter(out -> out != null).collect(Collectors.toSet());
        }
        return nearby;
    }

    private Set<DepartureDAO> _departuresDAO(Stop stop) {
        Instant utcNow = Instant.now();
        List<Departure> depList = Multi.createFrom().iterable(getDepartures(stop)).transform()
                .byFilteringItemsWith(
                        dep -> dep.getScheduled_departure_utc().isAfter(utcNow.minus(this.pastSeconds, ChronoUnit.SECONDS))
                                && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextSeconds, ChronoUnit.SECONDS)))
                .collectItems().asList().await().indefinitely();
        Set<Departure> departures = convertListToSet(depList);
        log.debug("Departure count: " + departures.size());

        Set<DepartureDAO> nearby = new HashSet<DepartureDAO>();
        if (!departures.isEmpty()) {
            nearby = departures.stream().map(dep -> {
                String cacheKey = String.format("%s-%s-%s-%s-%s-%s", dep.getRoute_id().toString(), stop.getRoute_type(), dep.getDirection_id().toString(), dep.getRun_id(), dep.getStop_id(), dep.getScheduled_departure_utc().toString());
                if (enableCache && departureDAOCache.containsKey(cacheKey)) {
                    return departureDAOCache.get(cacheKey);
                }
                Route route = getRouteById(dep.getRoute_id());
                Direction direction = getDirectionById(dep.getDirection_id(), dep.getRoute_id(), stop.getRoute_type());
                if (null == route || null == direction) {
                    return null;
                }
                DepartureDAO d = new DepartureDAO(
                        getRouteTypeName(stop.getRoute_type()),
                        route.getRoute_name(),
                        route.getRoute_number(),
                        direction.getDirection_name(),
                        stop.getStop_name(),
                        dep.getScheduled_departure_utc().toString(),
                        dep.getAt_platform(),
                        dep.getEstimated_departure_utc() == null ? null : dep.getEstimated_departure_utc().toString(),
                        dep.getPlatform_number() == null ? null : dep.getPlatform_number().toString(),
                        dep.getRoute_id(),
                        dep.getStop_id(),
                        dep.getRun_id(),
                        dep.getDirection_id()
                );
                if (enableCache) {
                    departureDAOCache.put(cacheKey, d, 60, TimeUnit.SECONDS);
                }
                return d;
            }).filter(out -> out != null).collect(Collectors.toSet());
        }
        return nearby;
    }
}
