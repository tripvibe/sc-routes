package com.redhat.labs.tripvibe.models;

public class Response {

    public Response() {
    }

    private ResponseStatus status;

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }
}
