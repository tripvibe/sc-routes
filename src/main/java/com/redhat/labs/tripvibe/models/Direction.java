package com.redhat.labs.tripvibe.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;

@RegisterForReflection
public class Direction implements Serializable {

    private String route_direction_description;
    private Integer direction_id;
    private String direction_name;
    private Integer route_id;
    private Integer route_type;

    public Direction() {
    }

    @ProtoFactory
    public Direction(String route_direction_description, Integer direction_id, String direction_name, Integer route_id, Integer route_type) {
        this.route_direction_description = route_direction_description;
        this.direction_id = direction_id;
        this.direction_name = direction_name;
        this.route_id = route_id;
        this.route_type = route_type;
    }

    @ProtoField(number = 1)
    public String getRoute_direction_description() {
        return route_direction_description;
    }

    public void setRoute_direction_description(String route_direction_description) {
        this.route_direction_description = route_direction_description;
    }

    @ProtoField(number = 2)
    public Integer getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(Integer direction_id) {
        this.direction_id = direction_id;
    }

    @ProtoField(number = 3)
    public String getDirection_name() {
        return direction_name;
    }

    public void setDirection_name(String direction_name) {
        this.direction_name = direction_name;
    }

    @ProtoField(number = 4)
    public Integer getRoute_id() {
        return route_id;
    }

    public void setRoute_id(Integer route_id) {
        this.route_id = route_id;
    }

    @ProtoField(number = 5)
    public Integer getRoute_type() {
        return route_type;
    }

    public void setRoute_type(Integer route_type) {
        this.route_type = route_type;
    }
}
