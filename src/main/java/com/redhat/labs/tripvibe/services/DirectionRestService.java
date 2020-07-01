package com.redhat.labs.tripvibe.services;

import com.redhat.labs.tripvibe.models.DirectionResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface DirectionRestService {
    @GET
    @Path("/{route_id}")
    @Produces(MediaType.APPLICATION_JSON)
    DirectionResponse directions(@PathParam int route_id, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
