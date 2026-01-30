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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PathUtilsTest {

    @Test
    void should_return_null_when_path_is_null() {
        assertNull(PathUtils.normalizePath(null));
    }

    @Test
    void should_return_empty_string_when_path_is_empty() {
        assertEquals("", PathUtils.normalizePath(""));
    }

    @Test
    void should_not_modify_single_slash() {
        assertEquals("/", PathUtils.normalizePath("/"));
    }

    @Test
    void should_normalize_double_slashes_to_single_slash() {
        assertEquals("/path", PathUtils.normalizePath("//path"));
    }

    @Test
    void should_normalize_multiple_consecutive_slashes() {
        assertEquals("/path/to/resource", PathUtils.normalizePath("/path//to///resource"));
    }

    @Test
    void should_normalize_path_with_double_slash_at_start() {
        assertEquals("/test-normalize/123/normalize", PathUtils.normalizePath("//test-normalize/123/normalize"));
    }

    @Test
    void should_normalize_path_with_double_slash_in_middle() {
        assertEquals("/test-normalize/123/normalize", PathUtils.normalizePath("/test-normalize//123/normalize"));
    }

    @Test
    void should_normalize_path_with_multiple_double_slashes() {
        assertEquals("/a/b/c/d", PathUtils.normalizePath("//a//b//c//d"));
    }

    @Test
    void should_normalize_path_with_triple_slashes() {
        assertEquals("/path/to/resource", PathUtils.normalizePath("/path///to/resource"));
    }

    @Test
    void should_not_modify_already_normalized_path() {
        String normalPath = "/path/to/resource";
        assertEquals(normalPath, PathUtils.normalizePath(normalPath));
    }

    @Test
    void should_normalize_path_with_query_string() {
        assertEquals("/path/resource?query=value", PathUtils.normalizePath("/path//resource?query=value"));
    }

    @Test
    void should_normalize_root_with_multiple_slashes() {
        assertEquals("/", PathUtils.normalizePath("///"));
    }
}
