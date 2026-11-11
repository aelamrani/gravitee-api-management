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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import type { Flow, FlowExecution, SaveOutput } from '@gravitee/graphene-policy-studio';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';

import { notify } from '../../../shared/notify';
import { updateApiProductFlows, updateApiProductPlanFlows } from '../services/apiProductFlows';
import { apiProductKeys } from '../utils/queryKeys';

interface CurrentProductFlows {
    commonFlows: readonly Flow[];
    flowExecution: FlowExecution;
}

/**
 * Saves the product flows, then the flows of the changed plans. Saving never deploys: the product detail is
 * refetched so its "out of sync" banner offers the deployment. Whatever the outcome, the studio data is
 * refetched, so a save that failed half-way shows what the server actually holds.
 */
export function useApiProductPolicyStudioSave(productId: string | undefined, current: CurrentProductFlows) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { commonFlows: currentFlows, flowExecution: currentFlowExecution } = current;

    return useCallback(
        async (output: SaveOutput) => {
            if (!env || !productId) return;
            const envId = env.id;
            const { commonFlows, flowExecution, plansToUpdate } = output;

            try {
                if (commonFlows || flowExecution) {
                    // The PUT replaces both the flows and the mode, so the unchanged one is sent as it is.
                    await updateApiProductFlows(envId, productId, {
                        flows: [...(commonFlows ?? currentFlows)],
                        flowExecution: flowExecution ?? currentFlowExecution,
                    });
                }
                if (plansToUpdate?.length) {
                    await updateApiProductPlanFlows(
                        envId,
                        productId,
                        plansToUpdate.map(plan => ({ id: plan.id, flows: [...plan.flows] })),
                    );
                }
            } catch (error) {
                notify.error(error, 'Failed to save the API product flows.');
                throw error;
            } finally {
                await Promise.all([
                    queryClient.invalidateQueries({ queryKey: apiProductKeys.flows(envId, productId) }),
                    queryClient.invalidateQueries({ queryKey: apiProductKeys.studioPlans(envId, productId) }),
                    queryClient.invalidateQueries({ queryKey: apiProductKeys.detail(envId, productId) }),
                ]);
            }
        },
        [env, productId, queryClient, currentFlows, currentFlowExecution],
    );
}
