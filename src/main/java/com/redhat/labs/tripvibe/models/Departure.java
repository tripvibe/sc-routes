package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.time.ZonedDateTime;

public class Departure {
    public Integer getStopId() {
        return stopId;
    }

    public void setStopId(Integer stopId) {
        this.stopId = stopId;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public Instant getScheduledDepartureUTC() {
        return scheduledDepartureUTC;
    }

    public void setScheduledDepartureUTC(Instant scheduledDepartureUTC) {
        this.scheduledDepartureUTC = scheduledDepartureUTC;
    }

    public Instant getEstimatedDepartureUTC() {
        return estimatedDepartureUTC;
    }

    public void setEstimatedDepartureUTC(Instant estimatedDepartureUTC) {
        this.estimatedDepartureUTC = estimatedDepartureUTC;
    }

    public Boolean getAtPlatform() {
        return atPlatform;
    }

    public void setAtPlatform(Boolean atPlatform) {
        this.atPlatform = atPlatform;
    }

    public Integer getPlatformNumber() {
        return platformNumber;
    }

    public void setPlatformNumber(Integer platformNumber) {
        this.platformNumber = platformNumber;
    }

    public Departure(Integer stopId, Integer routeId, Integer directionId, Instant scheduledDepartureUTC,
                     Instant estimatedDepartureUTC, Boolean atPlatform, Integer platformNumber, Integer runId) {
        this.stopId = stopId;
        this.routeId = routeId;
        this.directionId = directionId;
        this.scheduledDepartureUTC = scheduledDepartureUTC;
        this.estimatedDepartureUTC = estimatedDepartureUTC;
        this.atPlatform = atPlatform;
        this.platformNumber = platformNumber;
        this.runId = runId;
    }

    @JsonbProperty("stop_id")
    private Integer stopId;
    @JsonbProperty("route_id")
    private Integer routeId;
    @JsonbProperty("direction_id")
    private Integer directionId;
    @JsonbProperty("scheduled_departure_utc")
    private Instant scheduledDepartureUTC;
    @JsonbProperty("estimated_departure_utc")
    private Instant estimatedDepartureUTC;
    @JsonbProperty("at_platform")
    private Boolean atPlatform;
    @JsonbProperty("platform_number")
    private Integer platformNumber;

    public Integer getRunId() {
        return runId;
    }

    public void setRunId(Integer runId) {
        this.runId = runId;
    }

    @JsonbProperty("run_id")
    private Integer runId;

    public Departure() {}
}
