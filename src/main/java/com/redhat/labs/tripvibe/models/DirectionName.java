package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;

public class DirectionName implements Serializable {

    private String direction_id;
    private String direction_name;

    public DirectionName() {
    }

    @ProtoFactory
    public DirectionName(String direction_id, String direction_name) {
        this.direction_id = direction_id;
        this.direction_name = direction_name;
    }

    @ProtoField(number = 1)
    public String getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(String direction_id) {
        this.direction_id = direction_id;
    }

    @ProtoField(number = 2)
    public String getDirection_name() {
        return direction_name;
    }

    public void setDirection_name(String direction_name) {
        this.direction_name = direction_name;
    }
}
