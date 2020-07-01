package com.redhat.labs.tripvibe.services;

import com.redhat.labs.tripvibe.models.StopsResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface StopRestService {

    @GET
    @Path("/{latlong}")
    @Produces(MediaType.APPLICATION_JSON)
    StopsResponse stops(@PathParam String latlong, @QueryParam("max_distance") String maxDistance, @QueryParam("devid") String devid, @QueryParam("signature") String signature);
}
