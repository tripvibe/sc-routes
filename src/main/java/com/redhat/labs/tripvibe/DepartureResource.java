package com.redhat.labs.tripvibe;

import com.acme.util.Signature;
import com.redhat.labs.tripvibe.models.DepartureDAO;
import com.redhat.labs.tripvibe.models.Direction;
import com.redhat.labs.tripvibe.models.Route;
import com.redhat.labs.tripvibe.services.DepartureRestService;
import com.redhat.labs.tripvibe.services.DirectionRestService;
import com.redhat.labs.tripvibe.services.RouteRestService;
import com.redhat.labs.tripvibe.services.StopRestService;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/api")
@ApplicationScoped
public class DepartureResource {

    public static Map<Integer, Route> localRoutesCache = new HashMap<>();
    public static Map<Integer, Instant> localRoutesCacheAge = new HashMap<>();
    public static Map<String, Direction> localDirectionsCache = new HashMap<>();
    public static Map<String, Instant> localDirectionsCacheAge = new HashMap<>();
    private Integer maxCacheAgeHour = 24; //keep the cached objects upto 24 hours

    private final Logger log = LoggerFactory.getLogger(DepartureResource.class);

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

    @ConfigProperty(name = "com.acme.developerId")
    public String devid;

    @Inject
    Signature signature;

    @Inject
    RemoteCacheManager cacheManager;

    @ConfigProperty(name = "com.acme.enableCache")
    Boolean enableCache = false;

    //@Inject
    @Remote("routesCache")
    RemoteCache<Integer, Route> routesCache;

    //@Inject
    @Remote("directionsCache")
    RemoteCache<String, Direction> directionsCache;

    // default to next 3 hours
    // use to retrieve departures for the next 3 hours
    private int nextHours = 3;

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {

        if (!enableCache) return;

        log.info("On start - get caches");
        routesCache = cacheManager.administration().getOrCreateCache("routesCache", DefaultTemplate.REPL_ASYNC);
        directionsCache = cacheManager.administration().getOrCreateCache("directionsCache", DefaultTemplate.REPL_ASYNC);
        log.info("Existing stores are " + cacheManager.getCacheNames().toString());
    }

    @GET
    @Path("/nearby-departures/{latlong}/{distance}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<DepartureDAO> getNearbyDepartures(@PathParam String latlong, @PathParam String distance, @QueryParam Integer nextHours) {

        if (nextHours != null) this.nextHours = nextHours;

        log.info("Retrieving nearby departures...");
        var stops = stopsService.stops(latlong, distance, devid, signature.generate("/v3/stops/location/" + latlong + "?max_distance=" + distance)).getStops();
        if (stops.size() == 0) {
            return new HashSet<>(); // No stops nearby, return immediately
        }

        //0 = train, 1 = tram, 2 = bus, 3 = vline, 4 = night bus
        //cross join routeTypes and stops
        var routeTypeStops = stops.stream().flatMap(stop ->
                (IntStream.of(0, 1, 2, 3, 4)).mapToObj(routeType -> new ImmutablePair<>(stop, routeType)))
                    .collect(Collectors.toSet());

        var nearbyDepartures = new HashSet<DepartureDAO>();
        var utcNow = Instant.now();

        routeTypeStops.parallelStream().forEach(stop -> {
//            log.info("RouteType = " + stop.right);
//            log.info("Stop Id = " + stop.left.getStopId());
            var departures = departureService.departures(stop.right, stop.left.getStopId(),
                    devid, signature.generate("/v3/departures/route_type/" + stop.right + "/stop/" + stop.left.getStopId()))
                    .getDepartures()
                    //only return departures from NOW until the next few hours <nextHours> - defaults to 3
                    .stream().filter(dep ->
                            dep.getScheduledDepartureUTC().isAfter(utcNow.minus(1, ChronoUnit.MINUTES))
                            && dep.getScheduledDepartureUTC().isBefore(utcNow.plus(this.nextHours, ChronoUnit.HOURS)))
                    .collect(Collectors.toSet());

            if (!departures.isEmpty()) {
                var nearby = departures.stream().map(dep -> {
                    var route = getRouteById(dep.getRouteId());
                    var direction = getDirectionById(dep.getDirectionId(), dep.getRouteId(), stop.right);

                    return new DepartureDAO(
                            getRoutTypeName(stop.right),
                            route.getRouteName(),
                            route.getRouteNumber(),
                            direction.getDirectionName(),
                            stop.left.getStopName(),
                            dep.getScheduledDepartureUTC().toString(),
                            dep.getAtPlatform(),
                            dep.getEstimatedDepartureUTC() == null ? null : dep.getEstimatedDepartureUTC().toString(),
                            dep.getPlatformNumber() == null ? null : dep.getPlatformNumber().toString(),
                            dep.getRouteId(),
                            dep.getStopId(),
                            dep.getRunId(),
                            dep.getDirectionId()
                    );
                }).collect(Collectors.toSet());
                nearbyDepartures.addAll(nearby);
            }
        });

        return nearbyDepartures;
    }

    // this will not change in a decade
    private String getRoutTypeName(int type){
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
        //Use local in-memory cache
        if (!enableCache) {
            if (localRoutesCache.containsKey(routeId) && localRoutesCacheAge.containsKey(routeId)
                    && localRoutesCacheAge.get(routeId).isBefore(Instant.now().plus(maxCacheAgeHour, ChronoUnit.HOURS))) {
                return localRoutesCache.get(routeId);
            }
            var route = routeService.route(routeId, devid, signature.generate("/v3/routes/" + routeId)).getRoute();
            localRoutesCache.put(routeId, route);
            localRoutesCacheAge.put(routeId, Instant.now());
            return route;
        }

        if (routesCache.containsKey(routeId)) {
            return routesCache.get(routeId);
        }

        Route route = routeService.route(routeId, devid, signature.generate("/v3/routes/" + routeId)).getRoute();
        routesCache.put(routeId, route, 3600*12, TimeUnit.SECONDS);
        return route;
    }

    private Direction getDirectionById(int directionId, int routeId, int routeType) {
        var cacheKey = String.format("%s-%s-%s", directionId, routeId, routeType);

        // use local in-memory cache instead of infinispan
        if (!enableCache) {
            if (localDirectionsCache.containsKey(cacheKey) && localDirectionsCacheAge.containsKey(cacheKey)
                && localDirectionsCacheAge.get(cacheKey).isBefore(Instant.now().plus(maxCacheAgeHour, ChronoUnit.HOURS))){
                return localDirectionsCache.get(cacheKey);
            }

            var direction = directionService.directions(routeId, devid, signature.generate("/v3/directions/route/" + routeId))
                    .getDirections().stream().filter(d -> d.getDirectionId() == directionId && d.getRouteType() == routeType)
                    .findFirst().get();
            localDirectionsCache.put(cacheKey, direction);
            localDirectionsCacheAge.put(cacheKey, Instant.now());
            return direction;
        }


        if (directionsCache.containsKey((cacheKey))) {
            return directionsCache.get(cacheKey);
        }
        var directions = directionService.directions(routeId, devid, signature.generate("/v3/directions/route/" + routeId))
                .getDirections();
        var direction = directions.stream().filter(d -> d.getDirectionId() == directionId && d.getRouteType() == routeType)
                .findFirst().get();

        directionsCache.put(cacheKey, direction);
        return direction;
    }
}
