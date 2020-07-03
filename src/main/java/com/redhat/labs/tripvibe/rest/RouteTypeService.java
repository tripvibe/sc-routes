package com.redhat.labs.tripvibe.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface RouteTypeService {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    String routes(@QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
