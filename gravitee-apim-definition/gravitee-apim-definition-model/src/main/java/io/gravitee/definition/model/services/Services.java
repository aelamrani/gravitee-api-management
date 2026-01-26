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
package io.gravitee.definition.model.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.Service;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class Services implements Serializable {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/json",
        "application/xml",
        "text/plain",
        "text/xml",
        "text/html",
        "application/pdf",
        "image/png",
        "image/jpeg",
        "image/gif"
    );

    @JsonIgnore
    private final Map<Class<? extends Service>, Service> services = new HashMap<>();

    @JsonIgnore
    public Collection<Service> getAll() {
        return services.values();
    }

    @JsonIgnore
    public <T extends Service> T get(Class<T> serviceType) {
        //noinspection unchecked
        return (T) services.get(serviceType);
    }

    @JsonIgnore
    public void set(Collection<? extends Service> services) {
        services.forEach((Consumer<Service>) service -> Services.this.services.put(service.getClass(), service));
    }

    @JsonIgnore
    public void put(Class<? extends Service> clazz, Service service) {
        this.services.put(clazz, service);
    }

    @JsonIgnore
    public void remove(Class<? extends Service> clazz) {
        this.services.remove(clazz);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return services.isEmpty();
    }

    @JsonProperty("discovery")
    public EndpointDiscoveryService getDiscoveryService() {
        return get(EndpointDiscoveryService.class);
    }

    public void setDiscoveryService(EndpointDiscoveryService discoveryService) {
        if (discoveryService != null) {
            put(EndpointDiscoveryService.class, discoveryService);
        }
    }

    @JsonProperty("health-check")
    public HealthCheckService getHealthCheckService() {
        return get(HealthCheckService.class);
    }

    public void setHealthCheckService(HealthCheckService healthCheckService) {
        if (healthCheckService != null) {
            put(HealthCheckService.class, healthCheckService);
        }
    }

    @JsonProperty("dynamic-property")
    public DynamicPropertyService getDynamicPropertyService() {
        return get(DynamicPropertyService.class);
    }

    public void setDynamicPropertyService(DynamicPropertyService dynamicPropertyService) {
        if (dynamicPropertyService != null) {
            put(DynamicPropertyService.class, dynamicPropertyService);
        }
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream.of(
            Optional.ofNullable(this.getDiscoveryService())
                .filter(Service::isEnabled)
                .map(s -> new Plugin("service_discovery", s.getProvider())),
            Optional.ofNullable(this.getHealthCheckService())
                .filter(Service::isEnabled)
                .map(s -> new Plugin("service", "healthcheck")),
            Optional.ofNullable(this.getDynamicPropertyService())
                .filter(Service::isEnabled)
                .map(s -> new Plugin("service", "mgmt-service-dynamicproperties"))
        )
            .filter(Optional::isPresent)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    /**
     * Validates if the given MIME type is allowed for file uploads.
     *
     * @param mimeType the MIME type to validate
     * @return true if the MIME type is allowed, false otherwise
     */
    @JsonIgnore
    public static boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        return ALLOWED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    /**
     * Returns the set of allowed MIME types for file uploads.
     *
     * @return an unmodifiable set of allowed MIME types
     */
    @JsonIgnore
    public static Set<String> getAllowedMimeTypes() {
        return Collections.unmodifiableSet(ALLOWED_MIME_TYPES);
    }

    /**
     * Scans file content for potential malicious patterns.
     * This is a basic implementation that checks for common malicious signatures.
     *
     * @param content the file content as byte array
     * @return true if the content appears safe, false if potentially malicious
     */
    @JsonIgnore
    public static boolean scanFileContent(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }

        // Check for common executable signatures
        if (content.length >= 2) {
            // MZ header (Windows executable)
            if (content[0] == 0x4D && content[1] == 0x5A) {
                return false;
            }
            // ELF header (Linux executable)
            if (content.length >= 4 && content[0] == 0x7F && content[1] == 0x45 && 
                content[2] == 0x4C && content[3] == 0x46) {
                return false;
            }
        }

        // Check for script tags and other potentially dangerous content in text files
        String contentStr = new String(content);
        String lowerContent = contentStr.toLowerCase();
        
        // Check for potentially dangerous script patterns
        if (lowerContent.contains("<script") || 
            lowerContent.contains("javascript:") ||
            lowerContent.contains("vbscript:") ||
            lowerContent.contains("onload=") ||
            lowerContent.contains("onerror=")) {
            return false;
        }

        return true;
    }
}
