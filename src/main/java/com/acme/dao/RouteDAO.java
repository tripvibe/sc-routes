package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class RouteDAO {

    private String type;
    private String name;
    private String number;
    private String direction;
    private String stopName;
    private Double capacity;
    private Double vibe;
    private String departureTime;
    private String direction_id;
    private String route_id;

    public RouteDAO() {
    }

    @ProtoFactory
    public RouteDAO(String type, String name, String number, String direction, String stopName, Double capacity, Double vibe, String departureTime, String direction_id, String route_id) {
        this.type = type;
        this.name = name;
        this.number = number;
        this.direction = direction;
        this.stopName = stopName;
        this.capacity = capacity;
        this.vibe = vibe;
        this.departureTime = departureTime;
        this.direction_id = direction_id;
        this.route_id = route_id;
    }

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
    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(final Double capacity) {
        this.capacity = capacity;
    }

    @ProtoField(number = 7)
    public Double getVibe() {
        return vibe;
    }

    public void setVibe(final Double vibe) {
        this.vibe = vibe;
    }

    @ProtoField(number = 8)
    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(final String departureTime) {
        this.departureTime = departureTime;
    }

    @ProtoField(number = 9)
    public String getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(String direction_id) {
        this.direction_id = direction_id;
    }

    @ProtoField(number = 10)
    public String getRoute_id() {
        return route_id;
    }

    public void setRoute_id(String route_id) {
        this.route_id = route_id;
    }

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
                ", direction_id='" + direction_id + '\'' +
                ", route_id='" + route_id + '\'' +
                '}';
    }

}
