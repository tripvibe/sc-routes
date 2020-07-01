package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class RouteNameNumber {

    private String route_name;
    private String route_number;

    @ProtoFactory
    public RouteNameNumber(final String route_name, final String route_number) {
        this.route_name = route_name;
        this.route_number = route_number;
    }

    @ProtoField(number = 1)
    public String getRoute_name() {
        return route_name;
    }

    public void setRoute_name(final String route_name) {
        this.route_name = route_name;
    }

    @ProtoField(number = 2)
    public String getRoute_number() {
        return route_number;
    }

    public void setRoute_number(final String route_number) {
        this.route_number = route_number;
    }
}
