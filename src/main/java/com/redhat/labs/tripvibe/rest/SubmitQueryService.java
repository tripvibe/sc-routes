package com.redhat.labs.tripvibe.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface SubmitQueryService {

    @GET
    @Path("/vibe-average/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double vibeAverage(@PathParam String route_id);

    @GET
    @Path("/vibe-average/{route_id}/{direction_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double vibeAverageDestination(@PathParam String route_id, @PathParam String direction_id);

    @GET
    @Path("/vibe-average/{route_id}/{start}/{end}")
    @Produces(MediaType.APPLICATION_JSON)
    Double vibeAverageDatetime(@PathParam String route_id, @PathParam String start, @PathParam String end);

    @GET
    @Path("/vibe-average/{route_id}/{start}/{end}/{direction_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double vibeAverageDestinationDatetime(@PathParam String route_id, @PathParam String start, @PathParam String end, @PathParam String direction_id);

    @GET
    @Path("/capacity-average/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double capacityAverage(@PathParam String route_id);

    @GET
    @Path("/capacity-average/{route_id}/{direction_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double capacityAverageDestination(@PathParam String route_id, @PathParam String direction_id);

    @GET
    @Path("/capacity-average/{route_id}/{start}/{end}")
    @Produces(MediaType.APPLICATION_JSON)
    Double capacityAverageDatetime(@PathParam String route_id, @PathParam String start, @PathParam String end);

    @GET
    @Path("/capacity-average/{route_id}/{start}/{end}/{direction_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double capacityAverageDestinationDatetime(@PathParam String route_id, @PathParam String start, @PathParam String end, @PathParam String direction_id);

    @GET
    @Path("/capacity-average/{route_id}/{route_type}/{direction_id}/{run_id}/{stop_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double avgCapacityAllID(@PathParam String route_id, @PathParam String route_type, @PathParam String direction_id, @PathParam String run_id, @PathParam String stop_id);

    @GET
    @Path("/vibe-average/{route_id}/{route_type}/{direction_id}/{run_id}/{stop_id}")
    @Produces(MediaType.APPLICATION_JSON)
    Double avgVibeAllID(@PathParam String route_id, @PathParam String route_type, @PathParam String direction_id, @PathParam String run_id, @PathParam String stop_id);

}
