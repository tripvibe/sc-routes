package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Set;

public class Stops {

    private Set<Stop> stops;

    public Stops() {
    }

    @ProtoFactory
    public Stops(Set<Stop> stops) {
        this.stops = stops;
    }

    @ProtoField(number = 1)
    public Set<Stop> getStops() {
        return stops;
    }

    public void setStops(Set<Stop> stops) {
        this.stops = stops;
    }
}
