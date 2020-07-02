package com.redhat.labs.tripvibe.models;

import java.util.Set;

public class DepartureResponse extends Response {

    public DepartureResponse() {
    }

    public DepartureResponse(Set<Departure> departures) {
        this.departures = departures;
    }

    public Set<Departure> getDepartures() {
        return departures;
    }

    public void setDepartures(Set<Departure> departures) {
        this.departures = departures;
    }

    private Set<Departure> departures;

}
