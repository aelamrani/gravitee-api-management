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
package io.gravitee.gateway.flow.condition.evaluation;

/**
 * Utility class for normalizing request paths before flow matching.
 * Handles collapsing multiple consecutive slashes into a single slash.
 *
 * @author GraviteeSource Team
 */
public final class PathNormalizer {

    private PathNormalizer() {
        // Utility class
    }

    /**
     * Normalize the path by collapsing consecutive slashes.
     * For example: "//path" becomes "/path", "/path//to" becomes "/path/to"
     *
     * @param path the path to normalize
     * @return the normalized path, or the original path if null or empty
     */
    public static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        // Collapse multiple consecutive slashes into a single slash
        return path.replaceAll("/{2,}", "/");
    }
}
