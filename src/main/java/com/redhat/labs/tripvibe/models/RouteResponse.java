package com.redhat.labs.tripvibe.models;

public class RouteResponse extends Response {
    private Route route;

    public RouteResponse() {
    }

    public RouteResponse(Route route) {
        this.route = route;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }
}
