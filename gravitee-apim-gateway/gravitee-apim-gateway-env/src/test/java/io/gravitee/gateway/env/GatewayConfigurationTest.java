package io.gravitee.gateway.env;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class GatewayConfigurationTest {

    @Test
    public void testShardingTags() {
        GatewayConfiguration config = new GatewayConfiguration();
        config.initShardingTags();
        Optional<List<String>> tags = config.shardingTags();
        assertTrue(tags.isPresent());
        assertEquals(2, tags.get().size());
    }

    @Test
    public void testHasMatchingTags() {
        GatewayConfiguration config = new GatewayConfiguration();
        config.initShardingTags();
        assertTrue(config.hasMatchingTags(Set.of("tag1", "tag2")));
        assertFalse(config.hasMatchingTags(Set.of("tag3")));
    }
}