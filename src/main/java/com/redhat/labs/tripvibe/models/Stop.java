package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;

public class Stop {

    @JsonbProperty("stop_distance")
    private Double stopDistance;
    @JsonbProperty("stop_suburb")
    private String stopSuburb;
    @JsonbProperty("stop_name")
    private String stopName;

    public Stop() {}

    public Stop(Double stopDistance, String stopSuburb, String stopName, Integer stopId, Integer routeType, Double stopLatitude, Double stopLongitude, Integer stopSequence) {
        this.stopDistance = stopDistance;
        this.stopSuburb = stopSuburb;
        this.stopName = stopName;
        this.stopId = stopId;
        this.routeType = routeType;
        this.stopLatitude = stopLatitude;
        this.stopLongitude = stopLongitude;
        this.stopSequence = stopSequence;
    }

    @JsonbProperty("stop_id")
    private Integer stopId;

    public Double getStopDistance() {
        return stopDistance;
    }

    public void setStopDistance(Double stopDistance) {
        this.stopDistance = stopDistance;
    }

    public String getStopSuburb() {
        return stopSuburb;
    }

    public void setStopSuburb(String stopSuburb) {
        this.stopSuburb = stopSuburb;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public Integer getStopId() {
        return stopId;
    }

    public void setStopId(Integer stopId) {
        this.stopId = stopId;
    }

    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }

    public Double getStopLatitude() {
        return stopLatitude;
    }

    public void setStopLatitude(Double stopLatitude) {
        this.stopLatitude = stopLatitude;
    }

    public Double getStopLongitude() {
        return stopLongitude;
    }

    public void setStopLongitude(Double stopLongitude) {
        this.stopLongitude = stopLongitude;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

    @JsonbProperty("route_type")
    private Integer routeType;
    @JsonbProperty("stop_latitude")
    private Double stopLatitude;
    @JsonbProperty("stop_longitude")
    private Double stopLongitude;
    @JsonbProperty("stop_sequence")
    private Integer stopSequence;
}
