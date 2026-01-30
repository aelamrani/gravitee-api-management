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
package io.gravitee.gateway.reactive.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathUtils {

    /**
     * Normalize a path by collapsing multiple consecutive slashes into a single slash.
     * This is useful for ensuring consistent path matching behavior.
     *
     * Examples:
     * - "//path" becomes "/path"
     * - "/path//to///resource" becomes "/path/to/resource"
     * - "/" remains "/"
     *
     * @param path the path to normalize
     * @return the normalized path; null if the path is null; empty string if the path is empty
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        // Collapse multiple consecutive slashes into a single slash
        return path.replaceAll("/{2,}", "/");
    }
}
