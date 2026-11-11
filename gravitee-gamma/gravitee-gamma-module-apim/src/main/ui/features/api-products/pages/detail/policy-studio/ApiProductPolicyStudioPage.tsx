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
import { useLayoutConfig } from '@gravitee/graphene-core';
import { PolicyStudio } from '@gravitee/graphene-policy-studio';
import type { Policy, SaveOutput } from '@gravitee/graphene-policy-studio';
import { useCallback } from 'react';
import { useParams } from 'react-router-dom';

import { notify } from '../../../../../shared/notify';
import { getPolicyDocumentation, getPolicySchema } from '../../../../apis/services/policyStudioService';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { useApiProductPolicyStudioData } from '../../../hooks/useApiProductPolicyStudioData';
import { useApiProductPolicyStudioSave } from '../../../hooks/useApiProductPolicyStudioSave';

/** An API Product has no entrypoints or endpoints of its own: its flows run on the HTTP proxy APIs it holds. */
const NO_CONNECTORS = [] as const;

export function ApiProductPolicyStudioPage() {
    const { productId } = useParams<{ productId: string }>();

    useLayoutConfig({ contentVariant: 'full-bleed' }, []);

    const definition = useApiProductResourcePermissions(productId, 'DEFINITION');
    const plan = useApiProductResourcePermissions(productId, 'PLAN');
    const studioData = useApiProductPolicyStudioData(productId);
    const save = useApiProductPolicyStudioSave(productId, studioData);

    // The studio has a single read-only switch, so a plan edit without the plan permission is refused on save,
    // before anything is written.
    const canUpdatePlans = plan.canUpdate;
    const onSave = useCallback(
        async (output: SaveOutput) => {
            if (output.plansToUpdate?.length && !canUpdatePlans) {
                const error = new Error('You do not have permission to update plan flows.');
                notify.error(error);
                throw error;
            }
            await save(output);
        },
        [save, canUpdatePlans],
    );

    const onFetchPolicySchema = useCallback((policy: Policy) => getPolicySchema(policy.id, 'HTTP_PROXY'), []);
    const onFetchPolicyDocumentation = useCallback((policy: Policy) => getPolicyDocumentation(policy.id, 'HTTP_PROXY'), []);

    if (studioData.isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-destructive">Failed to load Policy Studio data. Please try again.</p>
            </div>
        );
    }

    return (
        <PolicyStudio
            apiType="PROXY"
            policies={studioData.policies}
            sharedPolicyGroups={studioData.sharedPolicyGroups}
            plans={studioData.plans}
            commonFlows={studioData.commonFlows}
            entrypointsInfo={NO_CONNECTORS}
            endpointsInfo={NO_CONNECTORS}
            flowExecution={studioData.flowExecution}
            readOnly={!definition.canUpdate}
            loading={studioData.isLoading || definition.isLoading || plan.isLoading}
            onSave={onSave}
            onFetchPolicySchema={onFetchPolicySchema}
            onFetchPolicyDocumentation={onFetchPolicyDocumentation}
        />
    );
}
