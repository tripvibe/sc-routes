package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Objects;

public class LatLongDistCacheKey implements Serializable {

    private String latlong;
    private String distance;

    public LatLongDistCacheKey() {
    }

    @ProtoFactory
    public LatLongDistCacheKey(String latlong, String distance) {
        this.latlong = latlong;
        this.distance = distance;
    }

    @ProtoField(number = 1)
    public String getLatlong() {
        return latlong;
    }

    public void setLatlong(String latlong) {
        this.latlong = latlong;
    }

    @ProtoField(number = 2)
    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatLongDistCacheKey routeDirectionCacheKey = (LatLongDistCacheKey) o;
        return Objects.equals(latlong, routeDirectionCacheKey.latlong) &&
                Objects.equals(distance, routeDirectionCacheKey.distance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latlong, distance);
    }

    @Override
    public String toString() {
        return "LatLongDistCacheKey{" +
                "latlong='" + latlong + '\'' +
                ", distance='" + distance + '\'' +
                '}';
    }
}
