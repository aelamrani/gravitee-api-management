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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProviderOrganizationBackfillUpgraderTest {

    @InjectMocks
    private ProviderOrganizationBackfillUpgrader upgrader = new ProviderOrganizationBackfillUpgrader();

    @Mock
    private ApiRepository apiRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(upgrader, "objectMapper", new ObjectMapper());
    }

    private void stubSearchWithSinglePage(Api... apis) {
        List<Api> content = List.of(apis);
        when(apiRepository.search(any(ApiCriteria.class), isNull(), any(Pageable.class), any(ApiFieldFilter.class))).thenReturn(
            new Page<>(content, 0, content.size(), content.size())
        );
    }

    private static Api federatedAgent(String id, String organization) {
        Api api = new Api();
        api.setId(id);
        api.setDefinitionVersion(DefinitionVersion.FEDERATED_AGENT);
        api.setDefinition(
            """
            {
              "name": "Agent",
              "description": "An agent",
              "url": "https://agent.example.com",
              "version": "1.0",
              "provider": { "organization": "%s", "url": "https://acme.example.com" }
            }
            """.formatted(organization)
        );
        return api;
    }

    private List<Api> federatedAgents(int count, String idPrefix) {
        List<Api> apis = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            apis.add(federatedAgent(idPrefix + i, "Acme Robotics"));
        }
        return apis;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providerOrganizationCases")
    void should_set_provider_organization_from_the_agent_definition(String caseName, String definition, String expectedOrganization)
        throws Exception {
        Api api = new Api();
        api.setId("api-1");
        api.setDefinitionVersion(DefinitionVersion.FEDERATED_AGENT);
        api.setProviderOrganization("Stale Corp");
        api.setDefinition(definition);

        stubSearchWithSinglePage(api);
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> invocation.getArgument(0));

        upgrader.applyUpgrade();

        verify(apiRepository).update(argThat(updated -> Objects.equals(updated.getProviderOrganization(), expectedOrganization)));
    }

    private static Stream<Arguments> providerOrganizationCases() {
        return Stream.of(
            Arguments.of(
                "should_set_provider_organization_from_the_agent_definition_provider",
                """
                {
                  "name": "Agent",
                  "description": "An agent",
                  "url": "https://agent.example.com",
                  "version": "1.0",
                  "provider": { "organization": "Acme Robotics", "url": "https://acme.example.com" }
                }
                """,
                "Acme Robotics"
            ),
            Arguments.of(
                "should_set_provider_organization_to_null_when_agent_has_no_provider",
                """
                {
                  "name": "Agent",
                  "description": "An agent",
                  "url": "https://agent.example.com",
                  "version": "1.0"
                }
                """,
                null
            ),
            Arguments.of(
                "should_set_provider_organization_to_null_when_agent_provider_has_no_organization",
                """
                {
                  "name": "Agent",
                  "description": "An agent",
                  "url": "https://agent.example.com",
                  "version": "1.0",
                  "provider": { "url": "https://acme.example.com" }
                }
                """,
                null
            )
        );
    }

    @Test
    void should_set_every_row_provider_organization_from_its_own_definition() throws Exception {
        stubSearchWithSinglePage(
            federatedAgent("api-1", "Acme Robotics"),
            federatedAgent("api-2", "Globex"),
            federatedAgent("api-3", "Initech")
        );
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> invocation.getArgument(0));

        upgrader.applyUpgrade();

        ArgumentCaptor<Api> updateCaptor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository, times(3)).update(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues())
            .extracting(Api::getId, Api::getProviderOrganization)
            .containsExactly(tuple("api-1", "Acme Robotics"), tuple("api-2", "Globex"), tuple("api-3", "Initech"));
    }

    @Test
    void should_preserve_the_other_row_columns_when_setting_provider_organization() throws Exception {
        Api api = federatedAgent("api-1", "Acme Robotics");
        api.setName("Agent API");
        api.setEnvironmentId("env-1");
        api.setIntegrationId("int-a");
        String seededDefinition = api.getDefinition();

        stubSearchWithSinglePage(api);
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> invocation.getArgument(0));

        upgrader.applyUpgrade();

        ArgumentCaptor<Api> updateCaptor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository).update(updateCaptor.capture());
        assertThat(updateCaptor.getValue())
            .extracting(
                Api::getId,
                Api::getName,
                Api::getEnvironmentId,
                Api::getIntegrationId,
                Api::getDefinition,
                Api::getDefinitionVersion,
                Api::getProviderOrganization
            )
            .containsExactly("api-1", "Agent API", "env-1", "int-a", seededDefinition, DefinitionVersion.FEDERATED_AGENT, "Acme Robotics");
    }

    @Test
    void should_restrict_the_search_to_federated_agent_apis() throws Exception {
        stubSearchWithSinglePage(federatedAgent("api-1", "Acme Robotics"));
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> invocation.getArgument(0));

        upgrader.applyUpgrade();

        ArgumentCaptor<ApiCriteria> criteriaCaptor = ArgumentCaptor.forClass(ApiCriteria.class);
        verify(apiRepository).search(criteriaCaptor.capture(), isNull(), any(Pageable.class), any(ApiFieldFilter.class));
        assertThat(criteriaCaptor.getValue().getDefinitionVersion()).containsExactly(DefinitionVersion.FEDERATED_AGENT);
    }

    @Test
    void should_stop_after_a_single_search_when_no_federated_agent_api_exists() throws Exception {
        stubSearchWithSinglePage();

        upgrader.applyUpgrade();

        verify(apiRepository).search(any(ApiCriteria.class), isNull(), any(Pageable.class), any(ApiFieldFilter.class));
        verify(apiRepository, never()).update(any(Api.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failingRowCases")
    void should_continue_with_the_next_row_when_a_row_fails(String caseName, Api failingRow, List<Tuple> expectedUpdates) throws Exception {
        stubSearchWithSinglePage(failingRow, federatedAgent("api-ok", "Globex"));
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> {
            Api updated = invocation.getArgument(0);
            if (failingRow.getId().equals(updated.getId())) {
                throw new TechnicalException("Failed to update api");
            }
            return updated;
        });

        upgrader.applyUpgrade();

        ArgumentCaptor<Api> updateCaptor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository, times(expectedUpdates.size())).update(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues())
            .extracting(Api::getId, Api::getProviderOrganization)
            .containsExactlyElementsOf(expectedUpdates);
    }

    private static Stream<Arguments> failingRowCases() {
        Api unparseable = new Api();
        unparseable.setId("api-broken");
        unparseable.setDefinitionVersion(DefinitionVersion.FEDERATED_AGENT);
        unparseable.setDefinition("not valid json");

        return Stream.of(
            Arguments.of(
                "should_skip_and_continue_with_the_next_row_when_a_definition_is_not_parseable",
                unparseable,
                List.of(tuple("api-ok", "Globex"))
            ),
            Arguments.of(
                "should_continue_with_the_next_row_when_persisting_a_row_fails",
                federatedAgent("api-persist-failure", "Acme Robotics"),
                List.of(tuple("api-persist-failure", "Acme Robotics"), tuple("api-ok", "Globex"))
            )
        );
    }

    @Test
    void should_report_the_upgrade_as_applied_when_a_definition_is_not_parseable() throws Exception {
        Api broken = new Api();
        broken.setId("api-broken");
        broken.setDefinitionVersion(DefinitionVersion.FEDERATED_AGENT);
        broken.setDefinition("not valid json");

        stubSearchWithSinglePage(broken);

        assertThat(upgrader.upgrade()).isTrue();
    }

    @Test
    void should_fail_the_upgrade_when_the_search_fails() {
        when(apiRepository.search(any(ApiCriteria.class), isNull(), any(Pageable.class), any(ApiFieldFilter.class))).thenThrow(
            new IllegalStateException("Repository is unavailable")
        );

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    @Test
    void should_persist_the_same_provider_organization_on_a_second_run() throws Exception {
        stubSearchWithSinglePage(federatedAgent("api-1", "Acme Robotics"));
        List<String> persistedOrganizations = new ArrayList<>();
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> {
            Api updated = invocation.getArgument(0);
            persistedOrganizations.add(updated.getProviderOrganization());
            return updated;
        });

        upgrader.applyUpgrade();
        upgrader.applyUpgrade();

        assertThat(persistedOrganizations).containsExactly("Acme Robotics", "Acme Robotics");
    }

    @Test
    void should_process_all_rows_across_multiple_bounded_size_pages() throws Exception {
        Map<Integer, List<Api>> apisByPageNumber = new HashMap<>();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(apiRepository.search(any(ApiCriteria.class), isNull(), pageableCaptor.capture(), any(ApiFieldFilter.class))).thenAnswer(
            invocation -> {
                Pageable pageable = invocation.getArgument(2, Pageable.class);
                List<Api> content = pageable.pageNumber() == 0
                    ? federatedAgents(pageable.pageSize(), "api-page0-")
                    : federatedAgents(1, "api-page1-");
                apisByPageNumber.put(pageable.pageNumber(), content);
                return new Page<>(content, pageable.pageNumber(), content.size(), pageable.pageSize() + 1);
            }
        );
        when(apiRepository.update(any(Api.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = upgrader.applyUpgrade();

        assertThat(result).isTrue();
        verify(apiRepository, times(2)).search(any(ApiCriteria.class), isNull(), any(Pageable.class), any(ApiFieldFilter.class));
        assertThat(pageableCaptor.getAllValues())
            .extracting(Pageable::pageNumber, Pageable::pageSize)
            .containsExactly(tuple(0, 100), tuple(1, 100));

        Set<String> expectedUpdatedIds = Stream.concat(apisByPageNumber.get(0).stream(), apisByPageNumber.get(1).stream())
            .map(Api::getId)
            .collect(Collectors.toSet());
        ArgumentCaptor<Api> updateCaptor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository, times(expectedUpdatedIds.size())).update(updateCaptor.capture());
        Set<String> actualUpdatedIds = updateCaptor.getAllValues().stream().map(Api::getId).collect(Collectors.toSet());
        assertThat(actualUpdatedIds).isEqualTo(expectedUpdatedIds);
    }
}
