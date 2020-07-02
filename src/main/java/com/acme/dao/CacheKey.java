package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Objects;

public class CacheKey implements Serializable {

    private String route_id;
    private String direction_id;

    public CacheKey() {
    }

    @ProtoFactory
    public CacheKey(String route_id, String direction_id) {
        this.route_id = route_id;
        this.direction_id = direction_id;
    }

    @ProtoField(number = 1)
    public String getRoute_id() {
        return route_id;
    }

    public void setRoute_id(String route_id) {
        this.route_id = route_id;
    }

    @ProtoField(number = 2)
    public String getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(String direction_id) {
        this.direction_id = direction_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(route_id, cacheKey.route_id) &&
                Objects.equals(direction_id, cacheKey.direction_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route_id, direction_id);
    }

    @Override
    public String toString() {
        return "CacheKey{" +
                "route_id='" + route_id + '\'' +
                ", direction_id='" + direction_id + '\'' +
                '}';
    }
}
