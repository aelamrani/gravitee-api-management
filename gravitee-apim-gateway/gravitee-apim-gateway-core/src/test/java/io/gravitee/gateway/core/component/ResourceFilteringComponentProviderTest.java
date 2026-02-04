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
package io.gravitee.gateway.core.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.resource.api.ResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link ResourceFilteringComponentProvider}.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResourceFilteringComponentProviderTest {

    @Mock
    private ComponentProvider delegateProvider;

    @Mock
    private ResourceManager resourceManager;

    private ResourceFilteringComponentProvider filteringProvider;

    @BeforeEach
    void setUp() {
        filteringProvider = new ResourceFilteringComponentProvider(delegateProvider);
    }

    @Test
    void should_filter_out_resource_manager_lookups() {
        // Given
        when(delegateProvider.getComponent(ResourceManager.class)).thenReturn(resourceManager);

        // When
        ResourceManager result = filteringProvider.getComponent(ResourceManager.class);

        // Then
        assertThat(result).isNull();
        verify(delegateProvider, never()).getComponent(ResourceManager.class);
    }

    @Test
    void should_allow_other_component_lookups() {
        // Given
        String testComponent = "test-component";
        when(delegateProvider.getComponent(String.class)).thenReturn(testComponent);

        // When
        String result = filteringProvider.getComponent(String.class);

        // Then
        assertThat(result).isEqualTo(testComponent);
        verify(delegateProvider).getComponent(String.class);
    }

    @Test
    void should_return_null_for_unknown_components() {
        // Given
        when(delegateProvider.getComponent(Integer.class)).thenReturn(null);

        // When
        Integer result = filteringProvider.getComponent(Integer.class);

        // Then
        assertThat(result).isNull();
        verify(delegateProvider).getComponent(Integer.class);
    }
}
