package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import javax.json.bind.annotation.JsonbProperty;

public class Route {

    @JsonbProperty("route_type")
    private Integer routeType;

    @JsonbProperty("route_id")
    private Integer routeId;

    @JsonbProperty("route_name")
    private String routeName;

    @JsonbProperty("route_number")
    private String routeNumber;

    public Route() {}

    @ProtoFactory
    public Route(Integer routeType, Integer routeId, String routeName, String routeNumber) {
        this.routeType = routeType;
        this.routeId = routeId;
        this.routeName = routeName;
        this.routeNumber = routeNumber;
    }

    @ProtoField(number = 1)
    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }

    @ProtoField(number = 2)
    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    @ProtoField(number = 3)
    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    @ProtoField(number = 4)
    public String getRouteNumber() {
        return routeNumber;
    }

    public void setRouteNumber(String routeNumber) {
        this.routeNumber = routeNumber;
    }
}
