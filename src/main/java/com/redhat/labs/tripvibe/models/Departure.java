package com.redhat.labs.tripvibe.models;

import java.time.Instant;

public class Departure {

    private Integer stop_id;
    private Integer route_id;
    private Integer direction_id;
    private Instant scheduled_departure_utc;
    private Instant estimated_departure_utc;
    private Boolean at_platform;
    private Integer platform_number;
    private Integer run_id;

    public Departure() {
    }

    public Departure(Integer stop_id, Integer route_id, Integer direction_id, Instant scheduled_departure_utc,
                     Instant estimated_departure_utc, Boolean at_platform, Integer platform_number, Integer run_id) {
        this.stop_id = stop_id;
        this.route_id = route_id;
        this.direction_id = direction_id;
        this.scheduled_departure_utc = scheduled_departure_utc;
        this.estimated_departure_utc = estimated_departure_utc;
        this.at_platform = at_platform;
        this.platform_number = platform_number;
        this.run_id = run_id;
    }

    public Integer getStop_id() {
        return stop_id;
    }

    public void setStop_id(Integer stop_id) {
        this.stop_id = stop_id;
    }

    public Integer getRoute_id() {
        return route_id;
    }

    public void setRoute_id(Integer route_id) {
        this.route_id = route_id;
    }

    public Integer getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(Integer direction_id) {
        this.direction_id = direction_id;
    }

    public Instant getScheduled_departure_utc() {
        return scheduled_departure_utc;
    }

    public void setScheduled_departure_utc(Instant scheduled_departure_utc) {
        this.scheduled_departure_utc = scheduled_departure_utc;
    }

    public Instant getEstimated_departure_utc() {
        return estimated_departure_utc;
    }

    public void setEstimated_departure_utc(Instant estimated_departure_utc) {
        this.estimated_departure_utc = estimated_departure_utc;
    }

    public Boolean getAt_platform() {
        return at_platform;
    }

    public void setAt_platform(Boolean at_platform) {
        this.at_platform = at_platform;
    }

    public Integer getPlatform_number() {
        return platform_number;
    }

    public void setPlatform_number(Integer platform_number) {
        this.platform_number = platform_number;
    }

    public Integer getRun_id() {
        return run_id;
    }

    public void setRun_id(Integer run_id) {
        this.run_id = run_id;
    }
}
