package com.acme.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface DirectionService {

    @GET
    @Path("/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    String directions(@PathParam String route_id, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
