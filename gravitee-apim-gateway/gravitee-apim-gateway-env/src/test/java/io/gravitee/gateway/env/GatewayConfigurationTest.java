package io.gravitee.gateway.env;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class GatewayConfigurationTest {

    @Test
    public void testHasMatchingTags() {
        GatewayConfiguration config = new GatewayConfiguration();
        Set<String> tags = Set.of("tag1", "tag2");
        assertTrue(config.hasMatchingTags(tags));
    }
}