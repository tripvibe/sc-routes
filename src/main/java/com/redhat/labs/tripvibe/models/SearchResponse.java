package com.redhat.labs.tripvibe.models;

import java.util.Set;

public class SearchResponse extends Response {

    public SearchResponse() {
    }

    public SearchResponse(Set<Stop> stops) {
        this.stops = stops;
    }

    public Set<Stop> getStops() {
        return stops;
    }

    public void setStops(Set<Stop> stops) {
        this.stops = stops;
    }

    private Set<Stop> stops;

}
