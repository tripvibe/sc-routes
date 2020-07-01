package com.redhat.labs.tripvibe.services;

import com.redhat.labs.tripvibe.models.RouteResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface RouteRestService {
    @GET
    @Path("/{routeId}")
    @Produces(MediaType.APPLICATION_JSON)
    RouteResponse route(@PathParam int routeId, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
