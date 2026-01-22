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

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import java.util.Collections;
import java.util.HashSet;
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
class DefaultManagedEndpointGroupTest {

    @Mock
    private EndpointGroup endpointGroupDefinition;

    @Mock
    private LoadBalancer loadBalancer;

    private DefaultManagedEndpointGroup managedEndpointGroup;

    @BeforeEach
    void setUp() {
        when(endpointGroupDefinition.getLoadBalancer()).thenReturn(loadBalancer);
        when(loadBalancer.getType()).thenReturn(LoadBalancerType.ROUND_ROBIN);
        managedEndpointGroup = new DefaultManagedEndpointGroup(endpointGroupDefinition);
    }

    @Test
    void shouldReturnEmptySetWhenNoEndpointsAdded() {
        Set<ConnectorMode> modes = managedEndpointGroup.supportedModes();
        assertNotNull(modes);
        assertTrue(modes.isEmpty());
    }

    @Test
    void shouldReturnSupportedModesAfterAddingEndpoint() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("endpoint1", false, 
            Set.of(ConnectorMode.SUBSCRIBE, ConnectorMode.PUBLISH));

        managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        Set<ConnectorMode> modes = managedEndpointGroup.supportedModes();
        assertNotNull(modes);
        assertEquals(2, modes.size());
        assertTrue(modes.contains(ConnectorMode.SUBSCRIBE));
        assertTrue(modes.contains(ConnectorMode.PUBLISH));
    }

    @Test
    void shouldMergeSupportedModesFromMultipleEndpoints() {
        ManagedEndpoint endpoint1 = createMockManagedEndpoint("endpoint1", false, 
            Set.of(ConnectorMode.SUBSCRIBE));
        ManagedEndpoint endpoint2 = createMockManagedEndpoint("endpoint2", false, 
            Set.of(ConnectorMode.PUBLISH));

        managedEndpointGroup.addManagedEndpoint(endpoint1);
        managedEndpointGroup.addManagedEndpoint(endpoint2);

        Set<ConnectorMode> modes = managedEndpointGroup.supportedModes();
        assertNotNull(modes);
        assertEquals(2, modes.size());
        assertTrue(modes.contains(ConnectorMode.SUBSCRIBE));
        assertTrue(modes.contains(ConnectorMode.PUBLISH));
    }

    @Test
    void shouldAddPrimaryEndpoint() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("primary1", false, 
            Set.of(ConnectorMode.SUBSCRIBE));

        ManagedEndpoint result = managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        assertNotNull(result);
        assertEquals(managedEndpoint, result);
    }

    @Test
    void shouldAddSecondaryEndpoint() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("secondary1", true, 
            Set.of(ConnectorMode.SUBSCRIBE));

        ManagedEndpoint result = managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        assertNotNull(result);
        assertEquals(managedEndpoint, result);
    }

    @Test
    void shouldRemovePrimaryEndpoint() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("endpoint1", false, 
            Set.of(ConnectorMode.SUBSCRIBE));
        managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        ManagedEndpoint removed = managedEndpointGroup.removeManagedEndpoint("endpoint1");

        assertNotNull(removed);
        assertEquals(managedEndpoint, removed);
    }

    @Test
    void shouldRemoveSecondaryEndpoint() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("secondary1", true, 
            Set.of(ConnectorMode.SUBSCRIBE));
        managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        ManagedEndpoint removed = managedEndpointGroup.removeManagedEndpoint("secondary1");

        assertNotNull(removed);
        assertEquals(managedEndpoint, removed);
    }

    @Test
    void shouldReturnNullWhenRemovingNonExistentEndpoint() {
        ManagedEndpoint removed = managedEndpointGroup.removeManagedEndpoint("nonexistent");
        assertNull(removed);
    }

    @Test
    void shouldReturnDefinition() {
        assertEquals(endpointGroupDefinition, managedEndpointGroup.getDefinition());
    }

    @Test
    void shouldReturnSupportedApiType() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("endpoint1", false, 
            Set.of(ConnectorMode.SUBSCRIBE));
        when(managedEndpoint.getConnector().supportedApi()).thenReturn(ApiType.PROXY);

        managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        assertEquals(ApiType.PROXY, managedEndpointGroup.supportedApi());
    }

    @Test
    void shouldReturnNullSupportedApiWhenNoEndpoints() {
        assertNull(managedEndpointGroup.supportedApi());
    }

    @Test
    void shouldReturnNextEndpointFromPrimaries() {
        ManagedEndpoint managedEndpoint = createMockManagedEndpoint("endpoint1", false, 
            Set.of(ConnectorMode.SUBSCRIBE));
        managedEndpointGroup.addManagedEndpoint(managedEndpoint);

        ManagedEndpoint next = managedEndpointGroup.next();

        assertNotNull(next);
        assertEquals(managedEndpoint, next);
    }

    @Test
    void shouldReturnNextEndpointFromSecondariesWhenNoPrimaries() {
        ManagedEndpoint secondaryEndpoint = createMockManagedEndpoint("secondary1", true, 
            Set.of(ConnectorMode.SUBSCRIBE));
        managedEndpointGroup.addManagedEndpoint(secondaryEndpoint);

        ManagedEndpoint next = managedEndpointGroup.next();

        assertNotNull(next);
        assertEquals(secondaryEndpoint, next);
    }

    @Test
    void shouldReturnNullWhenNoEndpoints() {
        ManagedEndpoint next = managedEndpointGroup.next();
        assertNull(next);
    }

    @Test
    void shouldHandleEmptySupportedModes() {
        ManagedEndpoint endpoint1 = createMockManagedEndpoint("endpoint1", false, 
            Set.of(ConnectorMode.SUBSCRIBE));
        ManagedEndpoint endpoint2 = createMockManagedEndpoint("endpoint2", false, 
            Collections.emptySet());

        managedEndpointGroup.addManagedEndpoint(endpoint1);
        managedEndpointGroup.addManagedEndpoint(endpoint2);

        Set<ConnectorMode> modes = managedEndpointGroup.supportedModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertTrue(modes.contains(ConnectorMode.SUBSCRIBE));
    }

    private ManagedEndpoint createMockManagedEndpoint(String name, boolean isSecondary, 
            Set<ConnectorMode> supportedModes) {
        ManagedEndpoint managedEndpoint = mock(ManagedEndpoint.class);
        Endpoint endpointDefinition = mock(Endpoint.class);
        EndpointConnector connector = mock(EndpointConnector.class);

        when(managedEndpoint.getDefinition()).thenReturn(endpointDefinition);
        when(managedEndpoint.getConnector()).thenReturn(connector);
        when(endpointDefinition.getName()).thenReturn(name);
        when(endpointDefinition.isSecondary()).thenReturn(isSecondary);
        when(connector.supportedModes()).thenReturn(supportedModes);

        return managedEndpoint;
    }
}
