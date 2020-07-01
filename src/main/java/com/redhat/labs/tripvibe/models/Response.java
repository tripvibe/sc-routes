package com.redhat.labs.tripvibe.models;

public class Response {
    private ResponseStatus status;

    public Response(){}
    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }
}
