package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;

public class RouteResponse extends Response {
    @JsonbProperty("route")
    private Route route;

    public RouteResponse(){}
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
