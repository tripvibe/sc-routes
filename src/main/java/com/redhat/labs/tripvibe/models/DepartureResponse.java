package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

public class DepartureResponse extends Response {
    public DepartureResponse(Set<Departure> departures) {
        this.departures = departures;
    }

    public Set<Departure> getDepartures() {
        return departures;
    }

    public void setDepartures(Set<Departure> departures) {
        this.departures = departures;
    }

    @JsonbProperty("departures")
    private Set<Departure> departures;

    public DepartureResponse(){}
}
