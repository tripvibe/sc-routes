package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class RouteType {

    private String route_type;
    private String route_type_name;

    @ProtoFactory
    public RouteType(String route_type, String route_type_name) {
        this.route_type = route_type;
        this.route_type_name = route_type_name;
    }

    @ProtoField(number = 1)
    public String getRoute_type() {
        return route_type;
    }

    public void setRoute_type(String route_type) {
        this.route_type = route_type;
    }

    @ProtoField(number = 2)
    public String getRoute_type_name() {
        return route_type_name;
    }

    public void setRoute_type_name(String route_type_name) {
        this.route_type_name = route_type_name;
    }
}
