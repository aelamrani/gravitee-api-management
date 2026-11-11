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
import type { Flow, FlowExecution } from '@gravitee/graphene-policy-studio';

import { apimFetchJsonV2 } from '../../../shared/api/apimClient';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export interface ApiProductFlows {
    flows: Flow[];
    flowExecution?: FlowExecution;
}

export interface ApiProductPlanFlows {
    planId: string;
    flows: Flow[];
}

const productBase = (productId: string) => `/api-products/${encodeURIComponent(productId)}`;

export async function getApiProductFlows(envId: string, productId: string): Promise<ApiProductFlows> {
    return apimFetchJsonV2<ApiProductFlows>(envId, `${productBase(productId)}/flows`);
}

export async function updateApiProductFlows(envId: string, productId: string, body: ApiProductFlows): Promise<void> {
    await apimFetchJsonV2(envId, `${productBase(productId)}/flows`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
}

export async function updateApiProductPlanFlows(envId: string, productId: string, plans: ApiProductPlanFlows[]): Promise<void> {
    await apimFetchJsonV2(envId, `${productBase(productId)}/plans/_flows`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(plans),
    });
}
