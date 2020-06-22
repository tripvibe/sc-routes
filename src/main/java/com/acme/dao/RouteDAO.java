package com.acme.dao;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class RouteDAO {

    @ProtoFactory
    public RouteDAO(String type, String name, String number, String direction) {
        this.type = type;
        this.name = name;
        this.number = number;
        this.direction = direction;
    }

    private String type;
    private String name;
    private String number;
    private String direction;

    @ProtoField(number = 1)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ProtoField(number = 2)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ProtoField(number = 3)
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @ProtoField(number = 4)
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
