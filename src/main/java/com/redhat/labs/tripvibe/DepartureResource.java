package com.redhat.labs.tripvibe;

import com.redhat.labs.tripvibe.models.*;
import com.redhat.labs.tripvibe.rest.SubmitQueryService;
import com.redhat.labs.tripvibe.services.*;
import com.redhat.labs.tripvibe.util.Signature;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.util.stream.IntStream;

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
    // use to retrieve departures for the next 3 hours
    private int nextHours = 3;
    private int pastHours = 0;

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
    RemoteCache<LatLongDistCacheKey, Stops> stopsCache;
    RemoteCache<String, Stops> searchCache;
    RemoteCache<RouteDirectionCacheKey, TripVibeDAO> tripVibeDAOCache;
    RemoteCache<RouteDirectionCacheKey, DepartureDAO> departureDAOCache;

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
    @Path("/evict-single/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void evictSingle(@PathParam String route_id) {
        log.info("Evicting vibe,capacity for: " + route_id);
        vibeCache.remove(route_id);
        capacityCache.remove(route_id);
    }

    private Set<Departure> getDepartures(ImmutablePair<Stop, Integer> stop) {
        Uni<DepartureResponse> mdr = Uni.createFrom().item(departureService.departures(stop.right, stop.left.getStop_id(),
                devid, signature.generate("/v3/departures/route_type/" + stop.right + "/stop/" + stop.left.getStop_id()))).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        return mdr.await().indefinitely().getDepartures();
    }

    @GET
    @Path("/nearby-departures/{latlong}/{distance}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TripVibeDAO> getNearbyDepartures(@PathParam String latlong, @PathParam String distance, @DefaultValue("0") @QueryParam Integer pastHours, @QueryParam Integer nextHours) {

        if (nextHours != null) this.nextHours = nextHours;
        if (pastHours != null) this.pastHours = pastHours;

        log.info("Retrieving departures by stop using latlong: " + latlong + " distance: " + distance + " with pastHours: " + this.pastHours + " nextHours: " + this.nextHours);

        LatLongDistCacheKey lldkey = new LatLongDistCacheKey(latlong, distance);
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

        //0 = train, 1 = tram, 2 = bus, 3 = vline, 4 = night bus
        //cross join routeTypes and stops
        Set<ImmutablePair<Stop, Integer>> routeTypeStops = stops.getStops().stream().flatMap(stop ->
                (IntStream.of(0, 1, 2, 3, 4)).mapToObj(routeType -> new ImmutablePair<>(stop, routeType)))
                .collect(Collectors.toSet());

        HashSet<TripVibeDAO> nearbyDepartures = new HashSet<TripVibeDAO>();
        Instant utcNow = Instant.now();

        routeTypeStops.parallelStream().forEach(stop -> {
            List<Departure> departures = Multi.createFrom().iterable(getDepartures(stop)).transform()
                    .byFilteringItemsWith(
                            dep -> dep.getScheduled_departure_utc().isAfter(utcNow.minus(this.pastHours, ChronoUnit.HOURS))
                                    && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextHours, ChronoUnit.HOURS)))
                    .collectItems().asList().await().indefinitely();

            log.debug("Departures count : " + departures.size());
            if (!departures.isEmpty()) {
                Set<TripVibeDAO> nearby = departures.stream().map(dep -> {
                    RouteDirectionCacheKey rdck = new RouteDirectionCacheKey(dep.getRoute_id().toString(), dep.getDirection_id().toString());
                    if (enableCache && tripVibeDAOCache.containsKey(rdck)) {
                        return tripVibeDAOCache.get(rdck);
                    }
                    Route route = getRouteById(dep.getRoute_id());
                    Direction direction = getDirectionById(dep.getDirection_id(), dep.getRoute_id(), stop.right);
                    if (null == route || null == direction) {
                        return null;
                    }
                    TripVibeDAO t = new TripVibeDAO(
                            route.getRoute_name(),
                            route.getRoute_number(),
                            direction.getDirection_name(),
                            stop.left.getStop_name(),
                            dep.getScheduled_departure_utc().toString(),
                            getRoutTypeName(stop.right),
                            dep.getAt_platform(),
                            dep.getEstimated_departure_utc() == null ? null : dep.getEstimated_departure_utc().toString(),
                            dep.getPlatform_number() == null ? null : dep.getPlatform_number().toString(),
                            dep.getRoute_id(),
                            dep.getStop_id(),
                            dep.getRun_id(),
                            dep.getDirection_id(),
                            capacityAverage(dep.getRoute_id().toString()),
                            vibeAverage(dep.getRoute_id().toString()));
                    tripVibeDAOCache.put(rdck, t, 60, TimeUnit.SECONDS);
                    return t;
                }).filter(out -> out != null && !out.equals(0)).collect(Collectors.toSet());
                nearbyDepartures.addAll(nearby);
            }
        });

        log.info("Nearby departures returning count: " + nearbyDepartures.size());
        return nearbyDepartures;
    }

    // this will not change in a decade
    private String getRoutTypeName(int type) {
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
                    .findFirst().get();
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
        Direction direction = directions.stream().filter(d -> d.getDirection_id() == directionId && d.getRoute_type() == routeType)
                .findFirst().get();

        directionsCache.put(cacheKey, direction);
        return direction;
    }

    @GET
    @Path("/search-departures/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<DepartureDAO> searchDepartures(@PathParam String term, @QueryParam int routeType, @DefaultValue("0") @QueryParam Integer pastHours, @QueryParam Integer nextHours) {

        if (nextHours != null) this.nextHours = nextHours;
        if (pastHours != null) this.pastHours = pastHours;

        log.info("Retrieving departures by stop using keyword: " + term + " with pastHours: " + this.pastHours + " nextHours: " + this.nextHours);

        Stops stops = new Stops();
        if (enableCache && searchCache.containsKey(term)) {
            stops = searchCache.get(term);
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
            searchCache.put(term, stops, 2, TimeUnit.HOURS);
            printCacheSizes();
        }

        //0 = train, 1 = tram, 2 = bus, 3 = vline, 4 = night bus
        //cross join routeTypes and stops
        var routeTypeStops = stops.getStops().stream().map(stop -> new ImmutablePair<>(stop, routeType))
                .collect(Collectors.toSet());

        HashSet<DepartureDAO> nearbyDepartures = new HashSet<DepartureDAO>();
        Instant utcNow = Instant.now();

        routeTypeStops.parallelStream().forEach(stop -> {
            List<Departure> departures = Multi.createFrom().iterable(getDepartures(stop)).transform()
                    .byFilteringItemsWith(
                            dep -> dep.getScheduled_departure_utc().isAfter(utcNow.minus(this.pastHours, ChronoUnit.HOURS))
                                    && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextHours, ChronoUnit.HOURS)))
                    .collectItems().asList().await().indefinitely();

            log.debug("Departure count: " + departures.size());
            if (!departures.isEmpty()) {
                Set<DepartureDAO> nearby = departures.stream().map(dep -> {
                    RouteDirectionCacheKey rdck = new RouteDirectionCacheKey(dep.getRoute_id().toString(), dep.getDirection_id().toString());
                    if (enableCache && departureDAOCache.containsKey(rdck)) {
                        return departureDAOCache.get(rdck);
                    }
                    Route route = getRouteById(dep.getRoute_id());
                    Direction direction = getDirectionById(dep.getDirection_id(), dep.getRoute_id(), stop.right);
                    DepartureDAO d = new DepartureDAO(
                            getRoutTypeName(stop.right),
                            route.getRoute_name(),
                            route.getRoute_number(),
                            direction.getDirection_name(),
                            stop.left.getStop_name(),
                            dep.getScheduled_departure_utc().toString(),
                            dep.getAt_platform(),
                            dep.getEstimated_departure_utc() == null ? null : dep.getEstimated_departure_utc().toString(),
                            dep.getPlatform_number() == null ? null : dep.getPlatform_number().toString(),
                            dep.getRoute_id(),
                            dep.getStop_id(),
                            dep.getRun_id(),
                            dep.getDirection_id()
                    );
                    departureDAOCache.put(rdck, d, 60, TimeUnit.SECONDS);
                    return d;
                }).collect(Collectors.toSet());
                nearbyDepartures.addAll(nearby);
            }
        });

        log.info("Nearby departures returning count: " + nearbyDepartures.size());
        return nearbyDepartures;
    }

    private Double capacityAverage(String route_id) {
        Double cap = -1.0;
        if (capacityCache.containsKey(route_id)) {
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
                log.error("capacityAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        capacityCache.put(route_id, cap, 1200, TimeUnit.SECONDS);
        return cap;
    }

    private Double vibeAverage(String route_id) {
        Double vib = -1.0;
        if (vibeCache.containsKey(route_id)) {
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
                log.error("vibeAverage - something went wrong " + e);
            }
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
        vibeCache.put(route_id, vib, 1200, TimeUnit.SECONDS);
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
}
