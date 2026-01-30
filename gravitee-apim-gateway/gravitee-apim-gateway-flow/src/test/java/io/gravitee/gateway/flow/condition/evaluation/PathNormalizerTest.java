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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PathNormalizerTest {

    @Test
    public void shouldReturnNull_whenPathIsNull() {
        assertNull(PathNormalizer.normalize(null));
    }

    @Test
    public void shouldReturnEmpty_whenPathIsEmpty() {
        assertEquals("", PathNormalizer.normalize(""));
    }

    @Test
    public void shouldNotModify_whenPathHasNoConsecutiveSlashes() {
        assertEquals("/path", PathNormalizer.normalize("/path"));
        assertEquals("/path/to/resource", PathNormalizer.normalize("/path/to/resource"));
    }

    @Test
    public void shouldCollapseDoubleSlashes() {
        assertEquals("/path", PathNormalizer.normalize("//path"));
        assertEquals("/path/to", PathNormalizer.normalize("/path//to"));
        assertEquals("/path/to/resource", PathNormalizer.normalize("/path/to//resource"));
    }

    @Test
    public void shouldCollapseMultipleSlashes() {
        assertEquals("/path", PathNormalizer.normalize("///path"));
        assertEquals("/path/to", PathNormalizer.normalize("/path///to"));
        assertEquals("/path/to/resource", PathNormalizer.normalize("////path////to////resource"));
    }

    @Test
    public void shouldHandleComplexPaths() {
        assertEquals("/test-normalize/123/normalize", PathNormalizer.normalize("/test-normalize//123/normalize"));
        assertEquals("/api/v1/users/123", PathNormalizer.normalize("/api///v1//users//123"));
    }

    @Test
    public void shouldPreserveTrailingSlash() {
        assertEquals("/path/", PathNormalizer.normalize("/path/"));
        assertEquals("/path/to/", PathNormalizer.normalize("/path//to/"));
    }

    @Test
    public void shouldHandleRootPath() {
        assertEquals("/", PathNormalizer.normalize("/"));
        assertEquals("/", PathNormalizer.normalize("//"));
        assertEquals("/", PathNormalizer.normalize("///"));
    }
}
