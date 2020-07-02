package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;

public class RouteType implements Serializable {

    private String route_type;
    private String route_type_name;

    public RouteType() {
    }

    @ProtoFactory
    public RouteType(final String route_type, final String route_type_name) {
        this.route_type = route_type;
        this.route_type_name = route_type_name;
    }

    @ProtoField(number = 1)
    public String getRoute_type() {
        return route_type;
    }

    public void setRoute_type(final String route_type) {
        this.route_type = route_type;
    }

    @ProtoField(number = 2)
    public String getRoute_type_name() {
        return route_type_name;
    }

    public void setRoute_type_name(final String route_type_name) {
        this.route_type_name = route_type_name;
    }
}
