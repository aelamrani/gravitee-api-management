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
package io.gravitee.apim.core.api.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.ProtocolVersion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedConfigurationMigrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SharedConfigurationMigration sharedConfigurationMigration;

    @BeforeEach
    void setUp() {
        sharedConfigurationMigration = new SharedConfigurationMigration(objectMapper);
    }

    @Test
    void should_convert_endpoint_group() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);
        group.setHttpClientSslOptions(new HttpClientSslOptions());
        var headers = List.of(new HttpHeader("X-Any-Header", "any header"));
        group.setHeaders(headers);
        group.setHttpProxy(new HttpProxy());

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode httpNode = json.get("http");
        assertThat(httpNode).isNotNull();
        assertThat(httpNode.get("version").asText()).isEqualTo(ProtocolVersion.HTTP_1_1.name());
        assertThat(json.get("ssl")).isNotNull();
        assertThat(json.get("headers")).isNotNull();
        assertThat(json.get("headers")).hasSize(1);
        JsonNode firstHeader = json.get("headers").get(0);
        assertThat(firstHeader.get("name").asText()).isEqualTo("X-Any-Header");
        assertThat(firstHeader.get("value").asText()).isEqualTo("any header");
        assertThat(json.get("proxy")).isNotNull();
    }

    @Test
    void should_not_set_properties_if_null() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);
        group.setHttpClientSslOptions(null);
        group.setHttpProxy(null);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        assertThat(json.has("http")).isTrue();
        assertThat(json.has("ssl")).isFalse();
        assertThat(json.has("headers")).isFalse();
        assertThat(json.has("proxy")).isFalse();
    }

    @Test
    void should_filter_proxy_fields_when_useSystemProxy_is_enabled() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);

        var proxy = new HttpProxy();
        proxy.setEnabled(true);
        proxy.setUseSystemProxy(true);
        proxy.setHost("some-host"); // This should be filtered out
        proxy.setPort(8080); // This should be filtered out
        group.setHttpProxy(proxy);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode proxyNode = json.get("proxy");
        assertThat(proxyNode).isNotNull();
        assertThat(proxyNode.get("enabled").asBoolean()).isTrue();
        assertThat(proxyNode.get("useSystemProxy").asBoolean()).isTrue();
        // host and port should not be present when useSystemProxy is true
        assertThat(proxyNode.has("host")).isFalse();
        assertThat(proxyNode.has("port")).isFalse();
        assertThat(proxyNode.has("username")).isFalse();
        assertThat(proxyNode.has("password")).isFalse();
        assertThat(proxyNode.has("type")).isFalse();
    }

    @Test
    void should_keep_proxy_fields_when_useSystemProxy_is_disabled() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);

        var proxy = new HttpProxy();
        proxy.setEnabled(true);
        proxy.setUseSystemProxy(false);
        proxy.setHost("my-proxy-host");
        proxy.setPort(3128);
        proxy.setUsername("proxyuser");
        proxy.setPassword("proxypass");
        group.setHttpProxy(proxy);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode proxyNode = json.get("proxy");
        assertThat(proxyNode).isNotNull();
        assertThat(proxyNode.get("enabled").asBoolean()).isTrue();
        assertThat(proxyNode.get("useSystemProxy").asBoolean()).isFalse();
        assertThat(proxyNode.get("host").asText()).isEqualTo("my-proxy-host");
        assertThat(proxyNode.get("port").asInt()).isEqualTo(3128);
        assertThat(proxyNode.get("username").asText()).isEqualTo("proxyuser");
        assertThat(proxyNode.get("password").asText()).isEqualTo("proxypass");
        assertThat(proxyNode.get("type").asText()).isEqualTo("HTTP");
    }

    @Test
    void should_remove_null_host_and_zero_port_when_useSystemProxy_is_disabled() throws JsonProcessingException {
        var group = new EndpointGroup();
        var options = new HttpClientOptions();
        options.setVersion(ProtocolVersion.HTTP_1_1);
        group.setHttpClientOptions(options);

        var proxy = new HttpProxy();
        proxy.setEnabled(true);
        proxy.setUseSystemProxy(false);
        // host is null and port is 0 by default
        group.setHttpProxy(proxy);

        var result = sharedConfigurationMigration.convert(group);
        JsonNode json = objectMapper.readTree(result);

        JsonNode proxyNode = json.get("proxy");
        assertThat(proxyNode).isNotNull();
        assertThat(proxyNode.get("enabled").asBoolean()).isTrue();
        assertThat(proxyNode.get("useSystemProxy").asBoolean()).isFalse();
        // null host and zero port should be removed
        assertThat(proxyNode.has("host")).isFalse();
        assertThat(proxyNode.has("port")).isFalse();
        assertThat(proxyNode.has("username")).isFalse();
        assertThat(proxyNode.has("password")).isFalse();
        assertThat(proxyNode.get("type").asText()).isEqualTo("HTTP");
    }
}
