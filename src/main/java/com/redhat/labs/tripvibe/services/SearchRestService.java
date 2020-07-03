package com.redhat.labs.tripvibe.services;

import com.redhat.labs.tripvibe.models.SearchResponse;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public interface SearchRestService {

    @GET
    @Path("/{search_term}")
    @Produces(MediaType.APPLICATION_JSON)
    SearchResponse search(@PathParam String search_term, @QueryParam Integer route_types, @QueryParam("devid") String devid, @QueryParam("signature") String signature);

}
