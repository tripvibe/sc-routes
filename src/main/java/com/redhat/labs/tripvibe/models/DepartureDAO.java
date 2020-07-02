package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

public class DepartureDAO {

    private String route_name;
    private String route_number;
    private String direction;
    private String stop_name;
    private String departure_time;
    private String route_type;
    private Boolean at_platform;
    private String estimated_departure_time;
    private String platform_number;
    private Integer route_id;
    private Integer stop_id;
    private Integer run_id;
    private Integer direction_id;

    public DepartureDAO() {
    }

    @ProtoFactory
    public DepartureDAO(String route_type, String route_name, String route_number, String direction, String stop_name,
                        String departure_time, Boolean at_platform, String estimated_departure_time,
                        String platform_number, Integer route_id, Integer stop_id, Integer run_id, Integer direction_id) {
        this.route_type = route_type;
        this.route_name = route_name;
        this.route_number = route_number;
        this.direction = direction;
        this.stop_name = stop_name;
        this.departure_time = departure_time;
        this.at_platform = at_platform;
        this.estimated_departure_time = estimated_departure_time;
        this.platform_number = platform_number;
        this.route_id = route_id;
        this.stop_id = stop_id;
        this.run_id = run_id;
        this.direction_id = direction_id;
    }

    @ProtoField(number = 1)
    public String getRoute_type() {
        return route_type;
    }

    public void setRoute_type(String route_type) {
        this.route_type = route_type;
    }

    @ProtoField(number = 2)
    public String getRoute_name() {
        return route_name;
    }

    public void setRoute_name(String route_name) {
        this.route_name = route_name;
    }

    @ProtoField(number = 3)
    public String getRoute_number() {
        return route_number;
    }

    public void setRoute_number(String route_number) {
        this.route_number = route_number;
    }

    @ProtoField(number = 4)
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @ProtoField(number = 5)
    public String getStop_name() {
        return stop_name;
    }

    public void setStop_name(String stop_name) {
        this.stop_name = stop_name;
    }

    @ProtoField(number = 6)
    public String getDeparture_time() {
        return departure_time;
    }

    public void setDeparture_time(String departure_time){ this.departure_time = departure_time; }

    @ProtoField(number = 7)
    public boolean getAt_platform() {
        return at_platform;
    }

    public void setAt_platform(boolean at_platform) {
        this.at_platform = at_platform;
    }

    @ProtoField(number = 8)
    public String getEstimated_departure_time() {
        return estimated_departure_time;
    }

    public void setEstimated_departure_time(String estimated_departure_time) {
        this.estimated_departure_time = estimated_departure_time;
    }

    @ProtoField(number = 9)
    public String getPlatform_number() {
        return platform_number;
    }

    public void setPlatform_number(String platform_number) {
        this.platform_number = platform_number;
    }

    @ProtoField(number = 10)
    public Integer getRoute_id() {
        return route_id;
    }

    public void setRoute_id(Integer route_id) {
        this.route_id = route_id;
    }

    @ProtoField(number = 11)
    public Integer getStop_id() {
        return stop_id;
    }

    public void setStop_id(Integer stop_id) {
        this.stop_id = stop_id;
    }

    @ProtoField(number = 12)
    public Integer getRun_id() {
        return run_id;
    }

    public void setRun_id(Integer run_id) {
        this.run_id = run_id;
    }

    @ProtoField(number = 13)
    public Integer getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(Integer direction_id) {
        this.direction_id = direction_id;
    }

}
