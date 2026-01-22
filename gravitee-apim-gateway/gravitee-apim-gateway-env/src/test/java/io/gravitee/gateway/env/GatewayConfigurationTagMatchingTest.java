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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for tag matching logic in GatewayConfiguration.
 * Specifically tests the DEFAULT organization gateway tagging behavior.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class GatewayConfigurationTagMatchingTest {

    @Mock
    private Configuration configuration;

    private GatewayConfiguration gatewayConfiguration;

    @BeforeEach
    void setUp() {
        gatewayConfiguration = new GatewayConfiguration();
        ReflectionTestUtils.setField(gatewayConfiguration, "configuration", configuration);
    }

    @Test
    void shouldMatchWhenNoShardingTagsConfigured() {
        // Given: No sharding tags configured (DEFAULT organization scenario)
        when(configuration.getProperty("tags")).thenReturn(null);
        gatewayConfiguration.afterPropertiesSet();

        Set<String> apiTags = new HashSet<>();
        apiTags.add("tag1");
        apiTags.add("tag2");

        // When/Then: Should match any tags when no sharding tags configured
        assertTrue(gatewayConfiguration.hasMatchingTags(apiTags));
    }

    @Test
    void shouldMatchWhenNoShardingTagsAndEmptyApiTags() {
        // Given: No sharding tags configured
        when(configuration.getProperty("tags")).thenReturn(null);
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should match when both are empty
        assertTrue(gatewayConfiguration.hasMatchingTags(Collections.emptySet()));
    }

    @Test
    void shouldMatchWhenNoShardingTagsAndNullApiTags() {
        // Given: No sharding tags configured
        when(configuration.getProperty("tags")).thenReturn(null);
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should match when api tags is null
        assertTrue(gatewayConfiguration.hasMatchingTags(null));
    }

    @Test
    void shouldMatchWhenShardingTagsMatchApiTags() {
        // Given: Sharding tags configured
        when(configuration.getProperty("tags")).thenReturn("tag1,tag2");
        gatewayConfiguration.afterPropertiesSet();

        Set<String> apiTags = new HashSet<>();
        apiTags.add("tag1");

        // When/Then: Should match when tags overlap
        assertTrue(gatewayConfiguration.hasMatchingTags(apiTags));
    }

    @Test
    void shouldNotMatchWhenShardingTagsDoNotMatchApiTags() {
        // Given: Sharding tags configured
        when(configuration.getProperty("tags")).thenReturn("tag1,tag2");
        gatewayConfiguration.afterPropertiesSet();

        Set<String> apiTags = new HashSet<>();
        apiTags.add("tag3");

        // When/Then: Should not match when no tags overlap
        assertFalse(gatewayConfiguration.hasMatchingTags(apiTags));
    }
}
