package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class RouteResponse {

    public Route route;

    public RouteResponse() {
    }

    @ProtoFactory
    public RouteResponse(Route route) {
        this.route = route;
    }
}
