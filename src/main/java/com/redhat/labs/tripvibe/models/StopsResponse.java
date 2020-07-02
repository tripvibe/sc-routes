package com.redhat.labs.tripvibe.models;

import java.util.Set;

public class StopsResponse extends Response {

    private Set<Stop> stops;

    public StopsResponse() {
    }

    public Set<Stop> getStops() {
        return stops;
    }

    public void setStops(Set<Stop> stops) {
        this.stops = stops;
    }

    public StopsResponse(Set<Stop> stops) {
        this.stops = stops;
    }
}
