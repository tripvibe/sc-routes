package com.redhat.labs.tripvibe.services;

import com.redhat.labs.tripvibe.models.DepartureResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface DepartureRestService {
    @GET
    @Path("/{route_type}/stop/{stop_id}")
    @Produces(MediaType.APPLICATION_JSON)
    DepartureResponse departures(@PathParam Integer route_type, @PathParam Integer stop_id, @QueryParam("devid") String devid, @QueryParam("signature") String signature);
}
