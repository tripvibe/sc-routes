package com.redhat.labs.tripvibe;

import com.redhat.labs.tripvibe.models.*;
import com.redhat.labs.tripvibe.rest.SubmitQueryService;
import com.redhat.labs.tripvibe.services.*;
import com.redhat.labs.tripvibe.util.Signature;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
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
import javax.ws.rs.core.Response;
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
    private int nextSeconds = 3600*2;
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

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        if (!enableCache) return;
        log.info("On start - get caches");
        routesCache = cacheManager.administration().getOrCreateCache("routesCache", DefaultTemplate.REPL_ASYNC);
        directionsCache = cacheManager.administration().getOrCreateCache("directionsCache", DefaultTemplate.REPL_ASYNC);
        vibeCache = cacheManager.administration().getOrCreateCache("vibeCache", DefaultTemplate.REPL_ASYNC);
        capacityCache = cacheManager.administration().getOrCreateCache("capacityCache", DefaultTemplate.REPL_ASYNC);
        stopsCache = cacheManager.administration().getOrCreateCache("stopsCache", DefaultTemplate.REPL_ASYNC);
        searchCache = cacheManager.administration().getOrCreateCache("searchCache", DefaultTemplate.REPL_ASYNC);
        tripVibeDAOCache = cacheManager.administration().getOrCreateCache("tripVibeDAOCache", DefaultTemplate.REPL_ASYNC);
        departureDAOCache = cacheManager.administration().getOrCreateCache("departureDAOCache", DefaultTemplate.REPL_ASYNC);
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
        }
        log.info("Stops count : " + stops.getStops().size());
        if (stops.getStops().size() == 0) {
            return new HashSet<>(); // No stops nearby, return immediately
        }
        if (enableCache) {
            stopsCache.put(lldkey, stops, 2, TimeUnit.HOURS);
            printCacheSizes();
        }

        HashSet<TripVibeDAO> nearbyDepartures = new HashSet<TripVibeDAO>();
        Instant utcNow = Instant.now();

        stops.getStops().parallelStream().forEach(stop -> {
            List<Departure> depList = Multi.createFrom().iterable(getDepartures(stop)).transform()
                    .byFilteringItemsWith(
                            dep -> dep.getScheduled_departure_utc().isAfter(utcNow.minus(this.pastSeconds, ChronoUnit.SECONDS))
                                    && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextSeconds, ChronoUnit.SECONDS)))
                    .collectItems().asList().await().indefinitely();
            Set<Departure> departures = convertListToSet(depList);
            log.debug("Departures count : " + departures.size());

            if (!departures.isEmpty()) {
                Set<TripVibeDAO> nearby = departures.stream().map(dep -> {
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
                }).filter(out -> out != null && !out.equals(0)).collect(Collectors.toSet());
                nearbyDepartures.addAll(nearby);
            }
        });

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
        routesCache.put(routeId, route.route, 3600 * 12, TimeUnit.SECONDS);
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
    public Set<DepartureDAO> searchDepartures(@PathParam String term, @QueryParam int routeType, @DefaultValue("0") @QueryParam Integer pastSeconds, @QueryParam Integer nextSeconds) {

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
        Instant utcNow = Instant.now();

        stops.getStops().parallelStream().forEach(stop -> {
            List<Departure> depList = Multi.createFrom().iterable(getDepartures(stop)).transform()
                    .byFilteringItemsWith(
                            dep -> dep.getScheduled_departure_utc().isAfter(utcNow.minus(this.pastSeconds, ChronoUnit.SECONDS))
                                    && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextSeconds, ChronoUnit.SECONDS)))
                    .collectItems().asList().await().indefinitely();
            Set<Departure> departures = convertListToSet(depList);
            log.debug("Departure count: " + departures.size());

            if (!departures.isEmpty()) {
                Set<DepartureDAO> nearby = departures.stream().map(dep -> {
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
                }).filter(out -> out != null && !out.equals(0)).collect(Collectors.toSet());
                nearbyDepartures.addAll(nearby);
            }
        });

        log.info("Nearby departures returning count: " + nearbyDepartures.size());
        return nearbyDepartures;
    }

    /*
    -- average vibe + capacity algorithm
        (1) try most restrictive (route_id, route_type, direction_id, run_id, stop_id)
        (2) then by (route_id)

        -- if we want to explore
        (3) also between time is available
        (4) and/or any combination of the above
    */
    private Double capacityAverage(String route_id, String route_type, String direction_id, String run_id, String stop_id) {
        Double cap = -1.0;
        String cacheKey = String.format("%s-%s-%s-%s-%s", route_id, route_type, direction_id, run_id, stop_id);
        if (enableCache && capacityCache.containsKey(cacheKey)) {
            return capacityCache.get(cacheKey);
        }
        try {
            Double ret = submitQueryService.avgCapacityAllID(route_id, route_type, direction_id, run_id, stop_id);
            if (null != ret) {
                cap = ret;
            } else {
                cap = capacityAverage(route_id);
            }
        } catch (javax.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()
                    || e.getResponse().getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                // OK nothing collected yet, return default
                log.debug("capacityAverage - nothing found: " + e.getResponse().getStatus());
            } else {
                log.debug("capacityAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        if (enableCache) {
            capacityCache.put(cacheKey, cap, 1200, TimeUnit.SECONDS);
        }
        log.debug("capacityAverage " + cacheKey + " " + cap);
        return cap;
    }

    private Double vibeAverage(String route_id, String route_type, String direction_id, String run_id, String stop_id) {
        Double vib = -1.0;
        String cacheKey = String.format("%s-%s-%s-%s-%s", route_id, route_type, direction_id, run_id, stop_id);
        if (enableCache && vibeCache.containsKey(cacheKey)) {
            return vibeCache.get(cacheKey);
        }
        try {
            Double ret = submitQueryService.avgVibeAllID(route_id, route_type, direction_id, run_id, stop_id);
            if (null != ret) {
                vib = ret;
            } else {
                vib = vibeAverage(route_id);
            }
        } catch (javax.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()
                    || e.getResponse().getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                // OK nothing collected yet, return default
                log.debug("vibeAverage - nothing found: " + e.getResponse().getStatus());
            } else {
                log.debug("vibeAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        if (enableCache) {
            vibeCache.put(cacheKey, vib, 1200, TimeUnit.SECONDS);
        }
        log.debug("vibeAverage " + cacheKey + " " + vib);
        return vib;
    }

    private Double capacityAverage(String route_id) {
        Double cap = -1.0;
        if (enableCache && capacityCache.containsKey(route_id)) {
            return capacityCache.get(route_id);
        }
        try {
            Double ret = submitQueryService.capacityAverage(route_id);
            if (null != ret) cap = ret;
        } catch (javax.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()
                    || e.getResponse().getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                // OK nothing collected yet, return default
                log.debug("capacityAverage - nothing found: " + e.getResponse().getStatus());
            } else {
                log.debug("capacityAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        if (enableCache && cap != -1.0) {
            capacityCache.put(route_id, cap, 1200, TimeUnit.SECONDS);
        }
        log.debug("capacityAverage " + route_id + " " + cap);
        return cap;
    }

    private Double vibeAverage(String route_id) {
        Double vib = -1.0;
        if (enableCache && vibeCache.containsKey(route_id)) {
            return vibeCache.get(route_id);
        }
        try {
            Double ret = submitQueryService.vibeAverage(route_id);
            if (null != ret) vib = ret;
        } catch (javax.ws.rs.WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()
                    || e.getResponse().getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                // OK nothing collected yet, return default
                log.debug("vibeAverage - nothing found: " + e.getResponse().getStatus());
            } else {
                log.debug("vibeAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        if (enableCache && vib != -1.0) {
            vibeCache.put(route_id, vib, 1200, TimeUnit.SECONDS);
        }
        log.debug("vibeAverage " + route_id + " " + vib);
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
}
