package com.redhat.labs.tripvibe.models;

import java.io.Serializable;

public class Response implements Serializable {

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
