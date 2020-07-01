package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoField;

import javax.json.bind.annotation.JsonbProperty;

public class Direction {


    @JsonbProperty("route_direction_description")
    private String routeDirectionDescription;

    @JsonbProperty("direction_id")
    private Integer directionId;

    @JsonbProperty("direction_name")
    private String directionName;

    @JsonbProperty("route_id")
    private Integer routeId;

    @JsonbProperty("route_type")
    private Integer routeType;

    public Direction(){}

    public Direction(String routeDirectionDescription, Integer directionId, String directionName, Integer routeId, Integer routeType) {
        this.routeDirectionDescription = routeDirectionDescription;
        this.directionId = directionId;
        this.directionName = directionName;
        this.routeId = routeId;
        this.routeType = routeType;
    }
    @ProtoField(number = 1)
    public String getRouteDirectionDescription() {
        return routeDirectionDescription;
    }

    public void setRouteDirectionDescription(String routeDirectionDescription) {
        this.routeDirectionDescription = routeDirectionDescription;
    }
    @ProtoField(number = 2)
    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }
    @ProtoField(number = 3)
    public String getDirectionName() {
        return directionName;
    }

    public void setDirectionName(String directionName) {
        this.directionName = directionName;
    }
    @ProtoField(number = 4)
    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }
    @ProtoField(number = 5)
    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }
}
