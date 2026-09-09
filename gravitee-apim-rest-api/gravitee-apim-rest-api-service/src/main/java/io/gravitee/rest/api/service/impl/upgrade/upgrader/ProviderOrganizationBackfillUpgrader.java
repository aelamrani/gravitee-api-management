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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class ProviderOrganizationBackfillUpgrader implements Upgrader {

    private static final int PAGE_SIZE = 100;

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    boolean applyUpgrade() {
        log.info("Starting provider organization backfill for federated agent APIs.");

        int updated = 0;
        int pageNumber = 0;
        Page<Api> page;

        do {
            var pageable = new PageableBuilder().pageNumber(pageNumber).pageSize(PAGE_SIZE).build();
            page = apiRepository.search(
                new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.FEDERATED_AGENT)).build(),
                null,
                pageable,
                new ApiFieldFilter.Builder().excludePicture().build()
            );

            for (Api api : page.getContent()) {
                try {
                    FederatedAgent agent = objectMapper.readValue(api.getDefinition(), FederatedAgent.class);
                    api.setProviderOrganization(agent.getProvider() == null ? null : agent.getProvider().organization());
                    apiRepository.update(api);
                    updated++;
                } catch (Exception e) {
                    log.warn("Failed to backfill provider organization for API {}", api.getId(), e);
                }
            }

            pageNumber++;
        } while (page.getContent().size() == PAGE_SIZE);

        log.info("Provider organization backfill for federated agent APIs completed, updated {} API(s).", updated);

        return true;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.PROVIDER_ORGANIZATION_BACKFILL_UPGRADER;
    }
}
