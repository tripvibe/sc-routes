package com.redhat.labs.tripvibe.models;

import java.io.Serializable;

public class Stop implements Serializable {

    private Double stop_distance;
    private String stop_suburb;
    private String stop_name;
    private Integer stop_id;
    private Integer route_type;
    private Double stop_latitude;
    private Double stop_longitude;
    private Integer stop_sequence;

    public Stop() {
    }

    public Stop(Double stop_distance, String stop_suburb, String stop_name, Integer stop_id, Integer route_type, Double stop_latitude, Double stop_longitude, Integer stop_sequence) {
        this.stop_distance = stop_distance;
        this.stop_suburb = stop_suburb;
        this.stop_name = stop_name;
        this.stop_id = stop_id;
        this.route_type = route_type;
        this.stop_latitude = stop_latitude;
        this.stop_longitude = stop_longitude;
        this.stop_sequence = stop_sequence;
    }

    public Double getStop_distance() {
        return stop_distance;
    }

    public void setStop_distance(Double stop_distance) {
        this.stop_distance = stop_distance;
    }

    public String getStop_suburb() {
        return stop_suburb;
    }

    public void setStop_suburb(String stop_suburb) {
        this.stop_suburb = stop_suburb;
    }

    public String getStop_name() {
        return stop_name;
    }

    public void setStop_name(String stop_name) {
        this.stop_name = stop_name;
    }

    public Integer getStop_id() {
        return stop_id;
    }

    public void setStop_id(Integer stop_id) {
        this.stop_id = stop_id;
    }

    public Integer getRoute_type() {
        return route_type;
    }

    public void setRoute_type(Integer route_type) {
        this.route_type = route_type;
    }

    public Double getStop_latitude() {
        return stop_latitude;
    }

    public void setStop_latitude(Double stop_latitude) {
        this.stop_latitude = stop_latitude;
    }

    public Double getStop_longitude() {
        return stop_longitude;
    }

    public void setStop_longitude(Double stop_longitude) {
        this.stop_longitude = stop_longitude;
    }

    public Integer getStop_sequence() {
        return stop_sequence;
    }

    public void setStop_sequence(Integer stop_sequence) {
        this.stop_sequence = stop_sequence;
    }

}
