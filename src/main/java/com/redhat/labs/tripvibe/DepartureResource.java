package com.redhat.labs.tripvibe;

import com.redhat.labs.tripvibe.rest.SubmitQueryService;
import com.redhat.labs.tripvibe.util.Signature;

import com.redhat.labs.tripvibe.models.DepartureDAO;
import com.redhat.labs.tripvibe.models.Direction;
import com.redhat.labs.tripvibe.models.Route;
import com.redhat.labs.tripvibe.services.*;
import com.redhat.labs.tripvibe.models.*;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.StartupEvent;
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

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        if (!enableCache) return;
        log.info("On start - get caches");
        cacheManager.administration().getOrCreateCache("routesCache", DefaultTemplate.REPL_ASYNC);
        cacheManager.administration().getOrCreateCache("directionsCache", DefaultTemplate.REPL_ASYNC);
        cacheManager.administration().getOrCreateCache("vibeCache", DefaultTemplate.REPL_ASYNC);
        cacheManager.administration().getOrCreateCache("capacityCache", DefaultTemplate.REPL_ASYNC);
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

    @GET
    @Path("/nearby-departures/{latlong}/{distance}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TripVibeDAO> getNearbyDepartures(@PathParam String latlong, @PathParam String distance, @QueryParam Integer nextHours) {

        if (nextHours != null) this.nextHours = nextHours;

        log.info("Retrieving nearby departures...");
        Set<Stop> stops = stopsService.stops(latlong, distance, devid, signature.generate("/v3/stops/location/" + latlong + "?max_distance=" + distance)).getStops();
        log.info("Stops count : " + stops.size());
        if (stops.size() == 0) {
            return new HashSet<>(); // No stops nearby, return immediately
        }

        log.info("Routes Cache contains " + routesCache.size() + " items ");
        log.info("Directions Cache contains " + directionsCache.size() + " items ");
        log.info("Vibe Cache contains " + vibeCache.size() + " items ");
        log.info("Capacity Cache contains " + capacityCache.size() + " items ");

        //0 = train, 1 = tram, 2 = bus, 3 = vline, 4 = night bus
        //cross join routeTypes and stops
        Set<ImmutablePair<Stop, Integer>> routeTypeStops = stops.stream().flatMap(stop ->
                (IntStream.of(0, 1, 2, 3, 4)).mapToObj(routeType -> new ImmutablePair<>(stop, routeType)))
                .collect(Collectors.toSet());

        HashSet<TripVibeDAO> nearbyDepartures = new HashSet<TripVibeDAO>();
        Instant utcNow = Instant.now();

        routeTypeStops.parallelStream().forEach(stop -> {
            Set<Departure> departures = departureService.departures(stop.right, stop.left.getStop_id(),
                    devid, signature.generate("/v3/departures/route_type/" + stop.right + "/stop/" + stop.left.getStop_id()))
                    .getDepartures().stream()
                    .filter(dep ->
                            dep.getScheduled_departure_utc().isAfter(utcNow.minus(1, ChronoUnit.MINUTES))
                                    && dep.getScheduled_departure_utc().isBefore(utcNow.plus(this.nextHours, ChronoUnit.HOURS)))
                    .collect(Collectors.toSet());

            log.debug("Departures count : " + departures.size());
            if (!departures.isEmpty()) {
                Set<TripVibeDAO> nearby = departures.stream().map(dep -> {
                    Route route = getRouteById(dep.getRoute_id());
                    Direction direction = getDirectionById(dep.getDirection_id(), dep.getRoute_id(), stop.right);
                    return new TripVibeDAO(
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
                }).collect(Collectors.toSet());
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
            Route route = routeService.route(routeId, devid, signature.generate("/v3/routes/" + routeId));
            localRoutesCache.put(routeId, route);
            localRoutesCacheAge.put(routeId, Instant.now());
            return route;
        }

        if (routesCache.containsKey(routeId)) {
            return routesCache.get(routeId);
        }

        Route route = routeService.route(routeId, devid, signature.generate("/v3/routes/" + routeId));
        routesCache.put(routeId, route, 3600 * 12, TimeUnit.SECONDS);
        return route;
    }

    private Direction getDirectionById(int directionId, int routeId, int routeType) {
        String cacheKey = String.format("%s-%s-%s", directionId, routeId, routeType);
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
        Direction direction = directions.stream().filter(d -> d.getDirection_id() == directionId && d.getRoute_type() == routeType)
                .findFirst().get();

        directionsCache.put(cacheKey, direction);
        return direction;
    }

    @GET
    @Path("/search-departures/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<DepartureDAO> searchDepartures(@PathParam String term, @QueryParam int routeType) {

        log.info("Retrieving departures by stop using keyword: " + term);

        Set<Stop> stops = searchService.search(term, routeType, devid,
                signature.generate("/v3/search/" + term.replace(" ","%20") + "?route_types=" + routeType)).getStops();

        log.info("Stops count : " + stops.size());
        if (stops.size() == 0) {
            return new HashSet<>(); // No stops nearby, return immediately
        }

        log.info("Routes Cache contains " + routesCache.size() + " items ");
        log.info("Directions Cache contains " + directionsCache.size() + " items ");
        log.info("Vibe Cache contains " + vibeCache.size() + " items ");
        log.info("Capacity Cache contains " + capacityCache.size() + " items ");

        //0 = train, 1 = tram, 2 = bus, 3 = vline, 4 = night bus
        //cross join routeTypes and stops
        var routeTypeStops = stops.stream().map(stop -> new ImmutablePair<>(stop, routeType))
                .collect(Collectors.toSet());

        HashSet<DepartureDAO> nearbyDepartures = new HashSet<DepartureDAO>();
        Instant utcNow = Instant.now();

        routeTypeStops.parallelStream().forEach(stop -> {
            Set<Departure> departures = departureService.departures(stop.right, stop.left.getStop_id(),
                    devid, signature.generate("/v3/departures/route_type/" + stop.right + "/stop/" + stop.left.getStop_id()))
                    .getDepartures().stream()
                    .filter(dep ->
                            dep.getScheduled_departure_utc().isAfter(utcNow.minus(1, ChronoUnit.MINUTES))
                                    && dep.getScheduled_departure_utc().isBefore(utcNow.plus(1, ChronoUnit.HOURS)))
                    .collect(Collectors.toSet());

            log.info("Departure count: " + departures.size());
            if (!departures.isEmpty()) {
                Set<DepartureDAO> nearby = departures.stream().map(dep -> {
                    Route route = getRouteById(dep.getRoute_id());
                    Direction direction = getDirectionById(dep.getDirection_id(), dep.getRoute_id(), stop.right);

                    return new DepartureDAO(
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
                }).collect(Collectors.toSet());
                nearbyDepartures.addAll(nearby);
            }
        });

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
}
