package com.redhat.labs.tripvibe.models;

import javax.json.bind.annotation.JsonbProperty;

public class ResponseStatus {
    @JsonbProperty("version")
    private String version;
    @JsonbProperty("health")
    private Integer health;

    public ResponseStatus() {}
    public ResponseStatus(String version, Integer health) {
        this.version = version;
        this.health = health;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }
}
