package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import javax.json.bind.annotation.JsonbProperty;

public class DepartureDAO {

    @ProtoFactory
    public DepartureDAO(String routeType, String routeName, String routeNumber, String direction, String stopName,
                        String departureTime, Boolean atPlatform, String estimatedDepartureTime,
                        String platformNumber, Integer routeID, Integer stopId, Integer runId, Integer directionId) {
        this.routeType = routeType;
        this.routeName = routeName;
        this.routeNumber = routeNumber;
        this.direction = direction;
        this.stopName = stopName;
        this.departureTime = departureTime;
        this.atPlatform = atPlatform;
        this.estimatedDepartureTime = estimatedDepartureTime;
        this.platformNumber = platformNumber;
        this.routeId = routeID;
        this.stopId = stopId;
        this.runId = runId;
        this.directionId = directionId;
    }

    @JsonbProperty("route_name")
    private String routeName;
    @JsonbProperty("route_number")
    private String routeNumber;
    @JsonbProperty("direction")
    private String direction;
    @JsonbProperty("stop_name")
    private String stopName;
    @JsonbProperty("departure_ime")
    private String departureTime;
    @JsonbProperty("route_type")
    private String routeType;
    @JsonbProperty("at_platform")
    private Boolean atPlatform;
    @JsonbProperty("estimated_departure_time")
    private String estimatedDepartureTime;
    @JsonbProperty("platform_number")
    private String platformNumber;
    @JsonbProperty("route_id")
    private Integer routeId;
    @JsonbProperty("stop_id")
    private Integer stopId;
    @JsonbProperty("run_id")
    private Integer runId;
    @JsonbProperty("direction_id")
    private Integer directionId;

    @ProtoField(number = 1)
    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    @ProtoField(number = 2)
    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    @ProtoField(number = 3)
    public String getRouteNumber() {
        return routeNumber;
    }

    public void setRouteNumber(String routeNumber) {
        this.routeNumber = routeNumber;
    }

    @ProtoField(number = 4)
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @ProtoField(number = 5)
    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    @ProtoField(number = 6)
    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime){ this.departureTime = departureTime; }

    @ProtoField(number = 7)
    public boolean isAtPlatform() {
        return atPlatform;
    }

    public void setAtPlatform(boolean atPlatform) {
        this.atPlatform = atPlatform;
    }

    @ProtoField(number = 8)
    public String getEstimatedDepartureTime() {
        return estimatedDepartureTime;
    }

    public void setEstimatedDepartureTime(String estimatedDepartureTime) {
        this.estimatedDepartureTime = estimatedDepartureTime;
    }

    @ProtoField(number = 9)
    public String getPlatformNumber() {
        return platformNumber;
    }

    public void setPlatformNumber(String platformNumber) {
        this.platformNumber = platformNumber;
    }

    @ProtoField(number = 10)
    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    @ProtoField(number = 11)
    public Integer getStopId() {
        return stopId;
    }

    public void setStopId(Integer stopId) {
        this.stopId = stopId;
    }

    @ProtoField(number = 12)
    public Integer getRunId() {
        return runId;
    }

    public void setRunId(Integer runId) {
        this.runId = runId;
    }

    @ProtoField(number = 13)
    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public DepartureDAO() {}
}
