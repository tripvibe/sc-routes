package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class RouteDAO {

    @Override
    public String toString() {
        return "RouteDAO{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", direction='" + direction + '\'' +
                ", stopName='" + stopName + '\'' +
                ", capacity=" + capacity +
                ", vibe=" + vibe +
                ", departureTime='" + departureTime + '\'' +
                '}';
    }

    @ProtoFactory
    public RouteDAO(final String type, final String name, final String number, final String direction,
            final String stopName, final Integer capacity, final Integer vibe, final String departureTime) {
        this.type = type;
        this.name = name;
        this.number = number;
        this.direction = direction;
        this.stopName = stopName;
        this.capacity = capacity;
        this.vibe = vibe;
        this.departureTime = departureTime;
    }

    private String type;
    private String name;
    private String number;
    private String direction;
    private String stopName;
    private Integer capacity;
    private Integer vibe;
    private String departureTime;

    @ProtoField(number = 1)
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @ProtoField(number = 2)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @ProtoField(number = 3)
    public String getNumber() {
        return number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    @ProtoField(number = 4)
    public String getDirection() {
        return direction;
    }

    public void setDirection(final String direction) {
        this.direction = direction;
    }

    @ProtoField(number = 5)
    public String getStopName() {
        return stopName;
    }

    public void setStopName(final String stopName) {
        this.stopName = stopName;
    }

    @ProtoField(number = 6)
    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(final Integer capacity) {
        this.capacity = capacity;
    }

    @ProtoField(number = 7)
    public Integer getVibe() {
        return vibe;
    }

    public void setVibe(final Integer vibe) {
        this.vibe = vibe;
    }

    @ProtoField(number = 8)
    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(final String departureTime) {
        this.departureTime = departureTime;
    }
}
