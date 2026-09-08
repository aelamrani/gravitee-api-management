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
package io.gravitee.apim.infra.query_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiQueryServiceImplTest {

    ApiRepository apiRepository;
    ApiQueryService service;

    @BeforeEach
    void setUp() {
        apiRepository = mock(ApiRepository.class);
        service = new ApiQueryServiceImpl(apiRepository);
    }

    @Test
    void search_should_return_matching_api_entities() {
        Api api = anApi();
        givenMatchingApis(Stream.of(api));

        var res = service
            .search(ApiSearchCriteria.builder().build(), Sortable.builder().build(), ApiFieldFilter.builder().build())
            .toList();
        assertThat(res).hasSize(1).containsExactly(api);
    }

    private void givenMatchingApis(Stream<Api> apis) {
        when(apiRepository.search(any(), any(), any())).thenReturn(ApiAdapter.INSTANCE.toRepositoryStream(apis));
    }

    private Api anApi() {
        return ApiFixtures.aProxyApiV4();
    }

    @Test
    void should_list_apis_matching_integration_id() {
        //Given
        var integrationId = "integration-id";
        var pageable = new PageableImpl(1, 5);

        var expectedApis = List.of(fixtures.repository.ApiFixtures.aFederatedApi());
        var page = new Page<>(expectedApis, pageable.getPageNumber(), expectedApis.size(), expectedApis.size());
        when(apiRepository.search(any(), any(), any(), any())).thenReturn(page);

        //When
        Page<Api> responsePage = service.findByIntegrationId(integrationId, pageable);

        //Then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responsePage).isNotNull();
            softly.assertThat(responsePage.getPageNumber()).isEqualTo(1);
            softly.assertThat(responsePage.getPageElements()).isEqualTo(1);
            softly.assertThat(responsePage.getTotalElements()).isEqualTo(1);
            softly.assertThat(responsePage.getContent().get(0).getId()).isEqualTo("api-id");
        });
    }

    @ParameterizedTest
    @MethodSource("integrationApiFilters")
    void should_apply_only_the_requested_filters_to_the_apis_of_an_integration(
        List<DefinitionVersion> requestedDefinitionVersions,
        String requestedQuery,
        List<DefinitionVersion> expectedDefinitionVersions,
        String expectedQuery
    ) {
        givenAnIntegrationApiPage();

        service.findByIntegrationId("int-a", requestedDefinitionVersions, requestedQuery, new PageableImpl(1, 5));

        var criteria = captureSearchCriteria();
        assertThat(criteria.getIntegrationId()).isEqualTo("int-a");
        assertThat(criteria.getDefinitionVersion()).isEqualTo(expectedDefinitionVersions);
        assertThat(criteria.getQuery()).isEqualTo(expectedQuery);
    }

    private static Stream<Arguments> integrationApiFilters() {
        return Stream.of(
            Arguments.of(List.of(DefinitionVersion.FEDERATED_AGENT), null, List.of(DefinitionVersion.FEDERATED_AGENT), null),
            Arguments.of(null, null, null, null),
            Arguments.of(List.of(), null, null, null),
            Arguments.of(null, "billing", null, "billing"),
            Arguments.of(null, "   ", null, null),
            Arguments.of(List.of(DefinitionVersion.FEDERATED_AGENT), "billing", List.of(DefinitionVersion.FEDERATED_AGENT), "billing")
        );
    }

    @Test
    void should_list_the_apis_of_an_integration_without_any_definition_version_or_query_filter() {
        givenAnIntegrationApiPage();

        service.findByIntegrationId("int-a", new PageableImpl(1, 5));

        var criteria = captureSearchCriteria();
        assertThat(criteria.getIntegrationId()).isEqualTo("int-a");
        assertThat(criteria.getDefinitionVersion()).isNull();
        assertThat(criteria.getQuery()).isNull();
    }

    @ParameterizedTest
    @MethodSource("integrationApiListings")
    void should_list_the_apis_of_an_integration_most_recently_updated_first(Consumer<ApiQueryService> listing) {
        givenAnIntegrationApiPage();

        listing.accept(service);

        var sortable = captureSortable();
        assertThat(sortable.field()).isEqualTo("updatedAt");
        assertThat(sortable.order()).isEqualTo(Order.DESC);
    }

    @ParameterizedTest
    @MethodSource("integrationApiListings")
    void should_list_the_apis_of_an_integration_without_their_definition(Consumer<ApiQueryService> listing) {
        givenAnIntegrationApiPage();

        listing.accept(service);

        assertThat(captureFieldFilter().isDefinitionExcluded()).isTrue();
    }

    private static Stream<Arguments> integrationApiListings() {
        return Stream.of(
            Arguments.of(
                Named.of("unfiltered", (Consumer<ApiQueryService>) service -> service.findByIntegrationId("int-a", new PageableImpl(1, 5)))
            ),
            Arguments.of(
                Named.of(
                    "filtered",
                    (Consumer<ApiQueryService>) service ->
                        service.findByIntegrationId("int-a", List.of(DefinitionVersion.FEDERATED_AGENT), "billing", new PageableImpl(1, 5))
                )
            )
        );
    }

    private void givenAnIntegrationApiPage() {
        var apis = List.of(fixtures.repository.ApiFixtures.aFederatedApi());
        when(apiRepository.search(any(), any(), any(), any())).thenReturn(new Page<>(apis, 1, apis.size(), apis.size()));
    }

    private ApiCriteria captureSearchCriteria() {
        var criteriaCaptor = ArgumentCaptor.forClass(ApiCriteria.class);
        verify(apiRepository).search(criteriaCaptor.capture(), any(), any(), any());
        return criteriaCaptor.getValue();
    }

    private io.gravitee.repository.management.api.search.Sortable captureSortable() {
        var sortableCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.api.search.Sortable.class);
        verify(apiRepository).search(any(), sortableCaptor.capture(), any(), any());
        return sortableCaptor.getValue();
    }

    private io.gravitee.repository.management.api.search.ApiFieldFilter captureFieldFilter() {
        var fieldFilterCaptor = ArgumentCaptor.forClass(io.gravitee.repository.management.api.search.ApiFieldFilter.class);
        verify(apiRepository).search(any(), any(), any(), fieldFilterCaptor.capture());
        return fieldFilterCaptor.getValue();
    }
}
