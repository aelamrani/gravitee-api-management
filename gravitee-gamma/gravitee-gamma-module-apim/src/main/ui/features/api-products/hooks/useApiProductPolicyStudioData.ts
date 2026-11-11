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
import type { Flow, FlowExecution, FlowPhase, Plan, Policy, SharedPolicyGroupPolicy } from '@gravitee/graphene-policy-studio';
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';

import { policyStudioKeys } from '../../apis/hooks/usePolicyStudioData';
import { listPlans } from '../../apis/services/plans';
import { listPolicies, listSharedPolicyGroupPlugins } from '../../apis/services/policyStudioService';
import { getApiProductFlows } from '../services/apiProductFlows';
import { apiProductKeys } from '../utils/queryKeys';

/** API Product flows run on the HTTP proxy APIs of the product, and only on their request and response. */
const PRODUCT_PHASES: readonly FlowPhase[] = ['REQUEST', 'RESPONSE'];

/** CLOSED plans are left out: their flows are read-only and the studio has no per-plan read-only (ADR D4). */
const STUDIO_PLAN_STATUSES = ['PUBLISHED', 'DEPRECATED'] as const;

export interface ApiProductPolicyStudioData {
    policies: readonly Policy[];
    sharedPolicyGroups: readonly SharedPolicyGroupPolicy[];
    plans: readonly Plan[];
    commonFlows: readonly Flow[];
    flowExecution: FlowExecution;
    isLoading: boolean;
    isError: boolean;
}

function toProductPolicy(plugin: Awaited<ReturnType<typeof listPolicies>>[number]): Policy | null {
    const phases = (plugin.flowPhaseCompatibility?.HTTP_PROXY ?? []).filter((phase): phase is FlowPhase =>
        PRODUCT_PHASES.includes(phase as FlowPhase),
    );
    if (phases.length === 0) return null;
    return {
        id: plugin.id,
        name: plugin.name,
        description: plugin.description,
        icon: plugin.icon,
        category: plugin.category,
        flowPhaseCompatibility: { HTTP_PROXY: phases },
        supportsMessageCondition: plugin.supportsMessageCondition,
    };
}

export function useApiProductPolicyStudioData(productId: string | undefined): ApiProductPolicyStudioData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && productId);

    const flowsQuery = useQuery({
        queryKey: apiProductKeys.flows(envId, productId ?? ''),
        queryFn: () => getApiProductFlows(envId, productId!),
        enabled,
    });

    const plansQuery = useQuery({
        queryKey: apiProductKeys.studioPlans(envId, productId ?? ''),
        // The studio renders every plan's flows at once; it has no paginated loading.
        queryFn: () => listPlans(envId, { type: 'api-product', entityId: productId! }, [...STUDIO_PLAN_STATUSES], 1, 9999),
        enabled,
    });

    const policiesQuery = useQuery({
        queryKey: policyStudioKeys.policies(),
        queryFn: listPolicies,
        enabled: Boolean(env),
        staleTime: 300_000,
    });

    const spgQuery = useQuery({
        queryKey: policyStudioKeys.sharedPolicyGroups(envId),
        queryFn: () => listSharedPolicyGroupPlugins(envId),
        enabled: Boolean(env),
        staleTime: 300_000,
        refetchOnMount: 'always',
    });

    const policies = useMemo(
        () => (policiesQuery.data ?? []).map(toProductPolicy).filter((policy): policy is Policy => policy !== null),
        [policiesQuery.data],
    );

    const sharedPolicyGroups: readonly SharedPolicyGroupPolicy[] = useMemo(
        () =>
            (spgQuery.data ?? [])
                .filter(spg => spg.apiType === 'PROXY' && PRODUCT_PHASES.includes(spg.phase as FlowPhase))
                .map(spg => ({
                    id: spg.id,
                    name: spg.name,
                    description: spg.description,
                    prerequisiteMessage: spg.prerequisiteMessage,
                    policyId: spg.policyId,
                    phase: spg.phase as SharedPolicyGroupPolicy['phase'],
                    apiType: 'PROXY',
                })),
        [spgQuery.data],
    );

    const plans: readonly Plan[] = useMemo(
        () => (plansQuery.data?.data ?? []).map(plan => ({ id: plan.id, name: plan.name, flows: (plan.flows ?? []) as readonly Flow[] })),
        [plansQuery.data],
    );

    const commonFlows: readonly Flow[] = useMemo(() => flowsQuery.data?.flows ?? [], [flowsQuery.data]);
    const flowExecution: FlowExecution = useMemo(() => flowsQuery.data?.flowExecution ?? { mode: 'DEFAULT' }, [flowsQuery.data]);

    return {
        policies,
        sharedPolicyGroups,
        plans,
        commonFlows,
        flowExecution,
        isLoading: flowsQuery.isLoading || plansQuery.isLoading || policiesQuery.isLoading || spgQuery.isLoading,
        isError: flowsQuery.isError || plansQuery.isError || policiesQuery.isError || spgQuery.isError,
    };
}
