package com.redhat.labs.tripvibe.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;

@RegisterForReflection
public class Route implements Serializable {

    private Integer route_type;
    private Integer route_id;
    private String route_name;
    private String route_number;

    public Route() {
    }

    @ProtoFactory
    public Route(Integer route_type, Integer route_id, String route_name, String route_number) {
        this.route_type = route_type;
        this.route_id = route_id;
        this.route_name = route_name;
        this.route_number = route_number;
    }

    @ProtoField(number = 1)
    public Integer getRoute_type() {
        return route_type;
    }

    public void setRoute_type(Integer route_type) {
        this.route_type = route_type;
    }

    @ProtoField(number = 2)
    public Integer getRoute_id() {
        return route_id;
    }

    public void setRoute_id(Integer route_id) {
        this.route_id = route_id;
    }

    @ProtoField(number = 3)
    public String getRoute_name() {
        return route_name;
    }

    public void setRoute_name(String route_name) {
        this.route_name = route_name;
    }

    @ProtoField(number = 4)
    public String getRoute_number() {
        return route_number;
    }

    public void setRoute_number(String route_number) {
        this.route_number = route_number;
    }
}
