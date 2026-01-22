/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.env;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GatewayConfiguration.
 */
public class GatewayConfigurationTest {

    private GatewayConfiguration gatewayConfiguration;

    @BeforeEach
    public void setUp() {
        gatewayConfiguration = new GatewayConfiguration();
        Configuration configuration = Mockito.mock(Configuration.class);
        gatewayConfiguration.configuration = configuration;
    }

    @Test
    public void shouldIncludeDefaultOrganizationWhenNoTags() {
        gatewayConfiguration.afterPropertiesSet();
        assertTrue(gatewayConfiguration.organizations().isPresent());
        assertEquals("default-organization", gatewayConfiguration.organizations().get().get(0));
    }

    @Test
    public void shouldMatchTagsForDefaultOrganization() {
        Set<String> tags = Set.of("default-tag");
        assertTrue(gatewayConfiguration.hasMatchingTags(tags));
    }
}
