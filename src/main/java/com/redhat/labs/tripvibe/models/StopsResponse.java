package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

public class StopsResponse extends Response {
    @JsonbProperty("stops")
    private Set<Stop> stops;

    public Set<Stop> getStops() {
        return stops;
    }

    public void setStops(Set<Stop> stops) {
        this.stops = stops;
    }

    public StopsResponse() {}
    public StopsResponse(Set<Stop> stops) {
        this.stops = stops;
    }
}
