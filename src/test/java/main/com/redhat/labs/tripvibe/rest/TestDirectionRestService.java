package main.com.redhat.labs.tripvibe.rest;

import com.redhat.labs.tripvibe.models.Direction;
import com.redhat.labs.tripvibe.models.DirectionResponse;
import com.redhat.labs.tripvibe.models.Stop;
import com.redhat.labs.tripvibe.models.StopsResponse;
import com.redhat.labs.tripvibe.services.DirectionRestService;
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
public class TestDirectionRestService {

    @Inject
    @RestClient
    DirectionRestService directionRestService;

    @Test
    void testDirectionRestService() {
        DirectionResponse directionResponse = directionRestService.directions(101, "devid", "signature");
        Assertions.assertTrue(directionResponse.getDirections().size() == 2);
        Set<Direction> directions = directionResponse.getDirections();
        directions.forEach(
                d -> {
                    if (d.getDirection_id() == 179) {
                        Assertions.assertTrue(d.getDirection_name().contains("Chadstone SC"));
                        Assertions.assertTrue(d.getRoute_id() == 8934);
                    }
                    if (d.getDirection_id() == 265) {
                        Assertions.assertTrue(d.getDirection_name().contains("Dandenong"));
                        Assertions.assertTrue(d.getRoute_id() == 8934);
                    }
                }
        );
    }
}
