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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class DefaultEndpointManagerTagsTest {

    @Mock
    private Api api;

    @Mock
    private EndpointConnectorPluginManager endpointConnectorPluginManager;

    @Mock
    private DeploymentContext deploymentContext;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private BaseEndpointConnectorFactory connectorFactory;

    @Mock
    private BaseEndpointConnector connector;

    private DefaultEndpointManager endpointManager;

    @BeforeEach
    void setUp() {
        when(gatewayConfiguration.tenant()).thenReturn(Optional.empty());
        endpointManager = new DefaultEndpointManager(api, endpointConnectorPluginManager, deploymentContext, gatewayConfiguration);
    }

    @Test
    void shouldStartEndpointWhenNoGatewayTagsConfigured() throws Exception {
        // Given
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        when(gatewayConfiguration.hasMatchingTags(any())).thenReturn(true);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getName()).thenReturn("test-endpoint");
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTags()).thenReturn(null);
        when(endpoint.getConfiguration()).thenReturn("{}");

        EndpointGroup endpointGroup = mock(EndpointGroup.class);
        when(endpointGroup.getName()).thenReturn("default-group");
        when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));

        when(api.getEndpointGroups()).thenReturn(List.of(endpointGroup));
        when(endpointConnectorPluginManager.getFactoryById("http")).thenReturn(connectorFactory);
        when(connectorFactory.createConnector(any(), any(), any())).thenReturn(connector);

        // When
        endpointManager.doStart();

        // Then
        assertNotNull(endpointManager.next());
    }

    @Test
    void shouldStartEndpointWhenTagsMatch() throws Exception {
        // Given
        Set<String> endpointTags = new HashSet<>(Arrays.asList("tag1"));
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("tag1", "tag2")));
        when(gatewayConfiguration.hasMatchingTags(endpointTags)).thenReturn(true);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getName()).thenReturn("test-endpoint");
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTags()).thenReturn(endpointTags);
        when(endpoint.getConfiguration()).thenReturn("{}");

        EndpointGroup endpointGroup = mock(EndpointGroup.class);
        when(endpointGroup.getName()).thenReturn("default-group");
        when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));

        when(api.getEndpointGroups()).thenReturn(List.of(endpointGroup));
        when(endpointConnectorPluginManager.getFactoryById("http")).thenReturn(connectorFactory);
        when(connectorFactory.createConnector(any(), any(), any())).thenReturn(connector);

        // When
        endpointManager.doStart();

        // Then
        assertNotNull(endpointManager.next());
    }

    @Test
    void shouldNotStartEndpointWhenTagsDoNotMatch() throws Exception {
        // Given
        Set<String> endpointTags = new HashSet<>(Arrays.asList("tag3"));
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("tag1", "tag2")));
        when(gatewayConfiguration.hasMatchingTags(endpointTags)).thenReturn(false);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getName()).thenReturn("test-endpoint");
        when(endpoint.getType()).thenReturn("http");
        when(endpoint.getTags()).thenReturn(endpointTags);

        EndpointGroup endpointGroup = mock(EndpointGroup.class);
        when(endpointGroup.getName()).thenReturn("default-group");
        when(endpointGroup.getEndpoints()).thenReturn(List.of(endpoint));

        when(api.getEndpointGroups()).thenReturn(List.of(endpointGroup));

        // When
        endpointManager.doStart();

        // Then
        assertNull(endpointManager.next());
    }
}
