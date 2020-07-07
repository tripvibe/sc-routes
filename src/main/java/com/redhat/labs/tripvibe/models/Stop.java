package com.redhat.labs.tripvibe.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Objects;

@RegisterForReflection
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

    @ProtoFactory
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

    @ProtoField(number = 1)
    public Double getStop_distance() {
        return stop_distance;
    }

    public void setStop_distance(Double stop_distance) {
        this.stop_distance = stop_distance;
    }

    @ProtoField(number = 2)
    public String getStop_suburb() {
        return stop_suburb;
    }

    public void setStop_suburb(String stop_suburb) {
        this.stop_suburb = stop_suburb;
    }

    @ProtoField(number = 3)
    public String getStop_name() {
        return stop_name;
    }

    public void setStop_name(String stop_name) {
        this.stop_name = stop_name;
    }

    @ProtoField(number = 4)
    public Integer getStop_id() {
        return stop_id;
    }

    public void setStop_id(Integer stop_id) {
        this.stop_id = stop_id;
    }

    @ProtoField(number = 5)
    public Integer getRoute_type() {
        return route_type;
    }

    public void setRoute_type(Integer route_type) {
        this.route_type = route_type;
    }

    @ProtoField(number = 6)
    public Double getStop_latitude() {
        return stop_latitude;
    }

    public void setStop_latitude(Double stop_latitude) {
        this.stop_latitude = stop_latitude;
    }

    @ProtoField(number = 7)
    public Double getStop_longitude() {
        return stop_longitude;
    }

    public void setStop_longitude(Double stop_longitude) {
        this.stop_longitude = stop_longitude;
    }

    @ProtoField(number = 8)
    public Integer getStop_sequence() {
        return stop_sequence;
    }

    public void setStop_sequence(Integer stop_sequence) {
        this.stop_sequence = stop_sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return Objects.equals(stop_distance, stop.stop_distance) &&
                Objects.equals(stop_suburb, stop.stop_suburb) &&
                Objects.equals(stop_name, stop.stop_name) &&
                Objects.equals(stop_id, stop.stop_id) &&
                Objects.equals(route_type, stop.route_type) &&
                Objects.equals(stop_latitude, stop.stop_latitude) &&
                Objects.equals(stop_longitude, stop.stop_longitude) &&
                Objects.equals(stop_sequence, stop.stop_sequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stop_distance, stop_suburb, stop_name, stop_id, route_type, stop_latitude, stop_longitude, stop_sequence);
    }
}
