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
package io.gravitee.definition.model;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.Services;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HttpRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequest.class);

    private String path;

    private HttpMethod method;

    private String body;

    private Map<String, List<String>> headers;

    /**
     * Validates the content type of an uploaded file.
     *
     * @param contentType the content type to validate
     * @return true if the content type is valid and allowed, false otherwise
     */
    public static boolean validateContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            LOGGER.warn("File upload rejected: Content-Type is missing");
            return false;
        }

        // Extract the base MIME type (without parameters like charset)
        String mimeType = contentType.split(";")[0].trim().toLowerCase();

        if (!Services.isAllowedMimeType(mimeType)) {
            LOGGER.warn("File upload rejected: Content-Type '{}' is not allowed. Allowed types: {}", 
                mimeType, Services.getAllowedMimeTypes());
            return false;
        }

        return true;
    }

    /**
     * Validates the file content for potential malicious patterns.
     *
     * @param content the file content as byte array
     * @return true if the content is safe, false if potentially malicious
     */
    public static boolean validateFileContent(byte[] content) {
        if (content == null || content.length == 0) {
            LOGGER.warn("File upload rejected: Content is empty");
            return false;
        }

        if (!Services.scanFileContent(content)) {
            LOGGER.warn("File upload rejected: Content contains potentially malicious patterns");
            return false;
        }

        return true;
    }

    /**
     * Validates both content type and file content for uploaded files.
     *
     * @param contentType the content type of the file
     * @param content the file content as byte array
     * @return true if both validations pass, false otherwise
     */
    public static boolean validateUploadedFile(String contentType, byte[] content) {
        return validateContentType(contentType) && validateFileContent(content);
    }
}
