package com.acme.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient
public interface DeparturesService {

    @GET
    @Path("/{route_type}/stop/{stop_id}")
    @Produces(MediaType.APPLICATION_JSON)
    String departures(@PathParam String route_type, @PathParam String stop_id, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
