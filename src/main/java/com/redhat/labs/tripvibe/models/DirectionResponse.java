package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;
import java.util.Set;

public class DirectionResponse extends Response {
    public Set<Direction> getDirections() {
        return directions;
    }

    public void setDirections(Set<Direction> directions) {
        this.directions = directions;
    }

    public DirectionResponse(Set<Direction> directions) {
        this.directions = directions;
    }

    @JsonbProperty("directions")
    private Set<Direction> directions;

    public DirectionResponse(){}
}
