package com.acme.dao;

import java.util.Objects;

public class Departure {

    private String route_type;
    private String stop_id;
    private String direction_id;
    private String scheduled_departure_utc;

    public Departure(String route_type, String stop_id, String direction_id, String scheduled_departure_utc) {
        this.route_type = route_type;
        this.stop_id = stop_id;
        this.direction_id = direction_id;
        this.scheduled_departure_utc = scheduled_departure_utc;
    }

    public String getRoute_type() {
        return route_type;
    }

    public void setRoute_type(String route_type) {
        this.route_type = route_type;
    }

    public String getStop_id() {
        return stop_id;
    }

    public void setStop_id(String stop_id) {
        this.stop_id = stop_id;
    }

    public String getDirection_id() {
        return direction_id;
    }

    public void setDirection_id(String direction_id) {
        this.direction_id = direction_id;
    }

    public String getScheduled_departure_utc() {
        return scheduled_departure_utc;
    }

    public void setScheduled_departure_utc(String scheduled_departure_utc) {
        this.scheduled_departure_utc = scheduled_departure_utc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Departure departure = (Departure) o;
        return Objects.equals(route_type, departure.route_type) &&
                Objects.equals(stop_id, departure.stop_id) &&
                Objects.equals(direction_id, departure.direction_id) &&
                Objects.equals(scheduled_departure_utc, departure.scheduled_departure_utc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route_type, stop_id, direction_id, scheduled_departure_utc);
    }
}
