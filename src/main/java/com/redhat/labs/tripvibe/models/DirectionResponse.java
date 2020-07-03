package com.redhat.labs.tripvibe.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Set;

@RegisterForReflection
public class DirectionResponse extends Response {

    public DirectionResponse() {
    }

    public Set<Direction> getDirections() {
        return directions;
    }

    public void setDirections(Set<Direction> directions) {
        this.directions = directions;
    }

    public DirectionResponse(Set<Direction> directions) {
        this.directions = directions;
    }

    private Set<Direction> directions;

}
