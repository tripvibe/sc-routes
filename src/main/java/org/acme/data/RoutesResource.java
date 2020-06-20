package org.acme.data;

import com.acme.rest.*;
import com.acme.util.Signature;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.SseElementType;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Path("/api")
public class RoutesResource {

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

    @GET
    @Path("/routes/{latlong}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<String> stream(@PathParam String latlong) {

        List<String> stops = Multi.createFrom().item(stopsService.routes(latlong, devid, signature.generate("/v3/stops/location/" + latlong))).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).collectItems().asList().await().indefinitely();
        List<Multi<String>> departures = Multi.createBy().merging().streams(
                Multi.createFrom().iterable(stops).map(
                        x -> Multi.createFrom().item(_departures(new JSONObject(x))).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()))
        ).collectItems().asList().await().indefinitely();

//        Multi trams = Multi.createFrom().iterable(Arrays.asList("{\"vehicle_type\" : \"Tram\", \"route_name\": \"Yarra\", \"stop_name\": \"Oakleigh SC/Atherton Rd\", \"direction_name\": \"City\" }"));
//        Multi buses = Multi.createFrom().iterable(Arrays.asList("{\"vehicle_type\" : \"Bus\", \"route_name\": \"Maxi\", \"stop_name\": \"Overton Rd\", \"direction_name\": \"Country\" }"));
//        Multi trains = Multi.createFrom().iterable(Arrays.asList("{\"vehicle_type\" : \"Train\", \"route_name\": \"Mikey\", \"stop_name\": \"The Edge\", \"direction_name\": \"City\" }"));
//
//        List<String> routes = (List<String>) Multi.createBy().merging().streams(trams, buses, trains)
//                .collectItems().asList()
//                .await().indefinitely();

        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(10)).onOverflow().drop();
        return ticks.onItem().produceMulti(
                x -> departures.iterator().next()
        ).merge();

    }

    @GET
    @Path("/stops/{latlong}")
    @Produces(MediaType.APPLICATION_JSON)
    public String stops(@PathParam String latlong) {
        return stopsService.routes(latlong, devid, signature.generate("/v3/stops/location/" + latlong));
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

    private String _departures(JSONObject obj) {
        // get departures based on geoloc
        JSONArray stops = obj.getJSONArray("stops");
        String route_type = null;
        String stop_id = null;
        for(int i=0;i<stops.length();i++) {
            JSONObject _stop = stops.getJSONObject(i);
            route_type = _stop.optString("route_type");
            stop_id = _stop.optString("stop_id");
        }

        // Service call for departures
        String departures = departuresService.departures(route_type, stop_id, devid, signature.generate("/v3/departures/route_type/" + route_type + "/stop/" + stop_id));

        // we only want unique route and direction
        JSONObject d = new JSONObject(departures);
        JSONArray deps = d.getJSONArray("departures");
        Map<String, String> _rd = new ConcurrentHashMap<String, String>();
        for(int i=0;i<deps.length();i++) {
            JSONObject _deps = deps.getJSONObject(i);
            _rd.put(_deps.optString("route_id"), _deps.optString("direction_id"));
        }
        List<JSONObject> jList = new ArrayList<JSONObject>();

        // RouteType
        final String rT = routeTypes(route_type);
        final String rTT = route_type;

        _rd.forEach((k, v) -> {
            JSONObject ret = new JSONObject();
            ret.put("Type", rT);
            ret.put("Name", routeName(k));
            ret.put("Direction", directionName(k,v));
            jList.add(ret);
        });

        return jList.toString();
    }

    private String routeTypes(String route_type) {
        // Route Type Service Call
        String routeTypes = routeTypeService.routes(devid, signature.generate("/v3/route_types"));
        JSONObject r = new JSONObject(routeTypes);
        JSONArray rts = r.getJSONArray("route_types");
        Map<String, String> _rt = new ConcurrentHashMap<String, String>();
        for(int i=0;i<rts.length();i++) {
            JSONObject _rts = rts.getJSONObject(i);
            _rt.put(_rts.optString("route_type"), _rts.optString("route_type_name"));
        }
        return _rt.get(route_type);
    }

    private String routeName(String route_id) {
        // Route Name Service Call
        String routeName = routeService.route(route_id, devid, signature.generate("/v3/routes/" + route_id));
        JSONObject r = new JSONObject(routeName);
        return r.getJSONObject("route").getString("route_name");
    }

    private String directionName(String route_id, String direction_id) {
        // Direction Name Service Call
        String directionName = directionService.directions(route_id, devid, signature.generate("/v3/directions/route/" + route_id));
        JSONObject r = new JSONObject(directionName);
        JSONArray rts = r.getJSONArray("directions");
        Map<String, String> _rt = new ConcurrentHashMap<String, String>();
        for(int i=0;i<rts.length();i++) {
            JSONObject _rts = rts.getJSONObject(i);
            _rt.put(_rts.optString("direction_id"), _rts.optString("direction_name"));
        }
        return _rt.get(direction_id);
    }
}
