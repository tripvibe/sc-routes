package main.com.redhat.labs.tripvibe.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockStopService implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        stubFor(get(urlPathMatching("/100*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("stop-response.json")));

        stubFor(get(urlPathMatching("/101*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("directions-response.json")));

        stubFor(get(urlMatching(".*")).atPriority(10)
                .willReturn(aResponse().proxiedFrom("https://timetableapi.ptv.vic.gov.au/v3/stops/location")));

        stubFor(get(urlMatching(".*")).atPriority(10)
                .willReturn(aResponse().proxiedFrom("https://timetableapi.ptv.vic.gov.au/v3/directions/route")));

        Map<String, String> urls = new HashMap<>();
        urls.put("com.redhat.labs.tripvibe.services.StopRestService/mp-rest/url", wireMockServer.baseUrl());
        urls.put("com.redhat.labs.tripvibe.services.DirectionRestService/mp-rest/url", wireMockServer.baseUrl());
        return urls;
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }
}
