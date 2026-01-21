package io.gravitee.apim.core.tag.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TagTest {

    @Test
    public void testDefaultOrganizationGatewayTagging() {
        Tag tag = new Tag();
        tag.setDefaultOrganizationGateway(true);
        assertTrue(tag.isDefaultOrganizationGateway());
    }

    // Additional tests for tagging logic
}
