package com.acme.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface RouteService {

    @GET
    @Path("/{routeId}")
    @Produces(MediaType.APPLICATION_JSON)
    String route(@PathParam String routeId, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
