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

import io.gravitee.resource.api.ResourceManager;
import lombok.extern.slf4j.Slf4j;

/**
 * A ComponentProvider wrapper that filters out ResourceManager lookups.
 * This ensures that policies always use the runtime ExecutionContext for resource access
 * rather than relying on a potentially shared/global ComponentProvider.
 * 
 * This is particularly important for shared policy groups where multiple APIs with resources
 * of the same name need to access their own API-specific resource instances.
 *
 * @author GraviteeSource Team
 */
@Slf4j
public class ResourceFilteringComponentProvider implements ComponentProvider {

    private final ComponentProvider delegate;

    public ResourceFilteringComponentProvider(ComponentProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T getComponent(Class<T> clazz) {
        // Filter out ResourceManager requests to force policies to use the ExecutionContext at runtime
        if (ResourceManager.class.isAssignableFrom(clazz)) {
            log.debug(
                "ResourceManager lookup blocked in shared context. " +
                "Resources must be accessed via ExecutionContext at runtime to ensure API-specific resource isolation."
            );
            return null;
        }
        return delegate.getComponent(clazz);
    }
}
