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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Organization;
import io.gravitee.node.api.configuration.Configuration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for GatewayConfiguration hasMatchingTags method.
 * Tests verify correct behavior with default organization gateways.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class GatewayConfigurationTest {

    @Mock
    private Configuration configuration;

    private GatewayConfiguration gatewayConfiguration;

    @BeforeEach
    void setUp() {
        gatewayConfiguration = new GatewayConfiguration(configuration);
    }

    @Test
    void shouldMatchWhenNoTagsConfigured() {
        // Given: Gateway has no tags configured
        when(configuration.getProperty("tags", String.class)).thenReturn(null);
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should match any tags (empty gateway tags means accept all)
        Set<String> apiTags = new HashSet<>(Arrays.asList("tag1", "tag2"));
        assertTrue(gatewayConfiguration.hasMatchingTags(apiTags));
    }

    @Test
    void shouldMatchWhenApiHasNoTags() {
        // Given: Gateway has tags but API has none
        when(configuration.getProperty("tags", String.class)).thenReturn("tag1,tag2");
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should match when API has no tags
        assertTrue(gatewayConfiguration.hasMatchingTags(Collections.emptySet()));
        assertTrue(gatewayConfiguration.hasMatchingTags(null));
    }

    @Test
    void shouldMatchWhenTagsOverlap() {
        // Given: Gateway has tags that overlap with API tags
        when(configuration.getProperty("tags", String.class)).thenReturn("tag1,tag2");
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should match when there's overlap
        Set<String> apiTags = new HashSet<>(Arrays.asList("tag1", "tag3"));
        assertTrue(gatewayConfiguration.hasMatchingTags(apiTags));
    }

    @Test
    void shouldNotMatchWhenNoTagsOverlap() {
        // Given: Gateway has tags that don't overlap with API tags
        when(configuration.getProperty("tags", String.class)).thenReturn("tag1,tag2");
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should not match when there's no overlap
        Set<String> apiTags = new HashSet<>(Arrays.asList("tag3", "tag4"));
        assertFalse(gatewayConfiguration.hasMatchingTags(apiTags));
    }

    @Test
    void shouldHandleExclusionTags() {
        // Given: Gateway has exclusion tags configured
        when(configuration.getProperty("tags", String.class)).thenReturn("tag1,!tag2");
        gatewayConfiguration.afterPropertiesSet();

        // When/Then: Should exclude APIs with excluded tags
        Set<String> apiTagsWithExcluded = new HashSet<>(Arrays.asList("tag1", "tag2"));
        assertFalse(gatewayConfiguration.hasMatchingTags(apiTagsWithExcluded));

        Set<String> apiTagsWithoutExcluded = new HashSet<>(Collections.singletonList("tag1"));
        assertTrue(gatewayConfiguration.hasMatchingTags(apiTagsWithoutExcluded));
    }

    @Test
    void shouldMatchForDefaultOrganizationGateway() {
        // Given: Default organization gateway with specific tags
        when(configuration.getProperty("tags", String.class)).thenReturn("internal");
        gatewayConfiguration.afterPropertiesSet();

        // When: API has matching tags
        Set<String> apiTags = new HashSet<>(Collections.singletonList("internal"));

        // Then: Should match
        assertTrue(gatewayConfiguration.hasMatchingTags(apiTags));
    }

    @Test
    void shouldNotMatchForDefaultOrganizationGatewayWithDifferentTags() {
        // Given: Default organization gateway with specific tags
        when(configuration.getProperty("tags", String.class)).thenReturn("internal");
        gatewayConfiguration.afterPropertiesSet();

        // When: API has different tags
        Set<String> apiTags = new HashSet<>(Collections.singletonList("external"));

        // Then: Should not match
        assertFalse(gatewayConfiguration.hasMatchingTags(apiTags));
    }
}
