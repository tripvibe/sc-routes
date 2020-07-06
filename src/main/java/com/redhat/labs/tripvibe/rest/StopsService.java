package com.redhat.labs.tripvibe.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public
interface StopsService {

    @GET
    @Path("/location/{latlong}")
    @Produces(MediaType.APPLICATION_JSON)
    String routes(@PathParam String latlong, @QueryParam("max_distance") String maxDistance, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

    @GET
    @Path("/route/{route_id}/route_type/{route_type}")
    @Produces(MediaType.APPLICATION_JSON)
    String routesById(@PathParam String route_id, @PathParam String route_type, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
