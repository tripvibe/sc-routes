package main.com.redhat.labs.tripvibe.rest;

import com.redhat.labs.tripvibe.models.Stop;
import com.redhat.labs.tripvibe.models.StopsResponse;
import com.redhat.labs.tripvibe.services.StopRestService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Set;

@QuarkusTest
@QuarkusTestResource(WireMockStopService.class)
public class TestStopRestService {

    @Inject
    @RestClient
    StopRestService stopRestService;

    @Test
    void testStopRestService() {
        StopsResponse stopsResponse = stopRestService.stops("100", "100", "devid", "signature");
        Assertions.assertTrue(stopsResponse.getStops().size() == 1);
        Set<Stop> stops = stopsResponse.getStops();
        stops.forEach(
                s -> {
                    Assertions.assertTrue(s.getStop_name().contains("Oakleigh SC/Atherton Rd "));
                    Assertions.assertTrue(s.getStop_id().equals(20420));
                }
        );
    }
}
