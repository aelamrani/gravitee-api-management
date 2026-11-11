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
import type { PolicyStudio, SaveOutput } from '@gravitee/graphene-policy-studio';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, waitFor } from '@testing-library/react';
import type { ComponentProps } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductPolicyStudioPage } from './ApiProductPolicyStudioPage';
import { resetApimClientForTests } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { TEST_CONFIG, TEST_V2_BASE } from '../../../../../testing/factories';
import { trackHandler } from '../../../../../testing/helpers';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));

jest.mock('@gravitee/graphene-core', () => ({ useLayoutConfig: jest.fn() }));

type PolicyStudioProps = ComponentProps<typeof PolicyStudio>;

let mockStudioProps: PolicyStudioProps | null = null;
jest.mock('@gravitee/graphene-policy-studio', () => ({
    PolicyStudio: (props: PolicyStudioProps) => {
        mockStudioProps = props;
        return <div data-testid="policy-studio-editor" />;
    },
}));

jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/product-1`;
const ORG_BASE = `${TEST_CONFIG.managementBaseURL}/v2/organizations/${TEST_CONFIG.organizationId}`;

const PRODUCT_FLOW = { id: 'flow-1', name: 'Product rate limit', enabled: true, request: [], response: [] };
const PLAN_FLOW = { id: 'flow-2', name: 'Gold plan flow', enabled: true, request: [], response: [] };

function serveStudio({ definition = 'RU', plan = 'RU' } = {}) {
    trackHandler('get', `${PRODUCT_PATH}/members/permissions`, { DEFINITION: definition, PLAN: plan });
    const flows = trackHandler('get', `${PRODUCT_PATH}/flows`, {
        flows: [PRODUCT_FLOW],
        flowExecution: { mode: 'BEST_MATCH', matchRequired: true },
    });
    const plans = trackHandler('get', `${PRODUCT_PATH}/plans`, {
        data: [{ id: 'plan-1', name: 'Gold', status: 'PUBLISHED', flows: [PLAN_FLOW] }],
        pagination: { totalCount: 1, pageIndex: 1, pageSize: 9999 },
    });
    trackHandler('get', `${ORG_BASE}/plugins/policies`, [
        { id: 'rate-limit', name: 'Rate Limit', flowPhaseCompatibility: { HTTP_PROXY: ['REQUEST'], HTTP_MESSAGE: ['PUBLISH'] } },
        { id: 'transform-headers', name: 'Transform Headers', flowPhaseCompatibility: { HTTP_PROXY: ['REQUEST', 'RESPONSE'] } },
        { id: 'message-filter', name: 'Message Filter', flowPhaseCompatibility: { HTTP_MESSAGE: ['SUBSCRIBE'] } },
    ]);
    trackHandler('get', `${TEST_V2_BASE}/shared-policy-groups/policy-plugins`, [
        { id: 'spg-proxy', name: 'Proxy group', policyId: 'shared-policy-group-policy', phase: 'REQUEST', apiType: 'PROXY' },
        { id: 'spg-message', name: 'Message group', policyId: 'shared-policy-group-policy', phase: 'PUBLISH', apiType: 'MESSAGE' },
    ]);
    return { flows, plans };
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/api-products/product-1/policy-studio']}>
                <Routes>
                    <Route path="/api-products/:productId/policy-studio" element={<ApiProductPolicyStudioPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

async function loadedStudio(): Promise<PolicyStudioProps> {
    await waitFor(() => {
        expect(mockStudioProps?.loading).toBe(false);
        expect(mockStudioProps?.plans).toHaveLength(1);
    });
    return mockStudioProps!;
}

const SAVE_BOTH: SaveOutput = {
    commonFlows: [{ ...PRODUCT_FLOW, enabled: false }],
    plansToUpdate: [{ id: 'plan-1', name: 'Gold', flows: [] }],
};

describe('ApiProductPolicyStudioPage', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockStudioProps = null;
        (useEnvironment as jest.Mock).mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
    });

    afterEach(() => jest.clearAllMocks());

    it('renders the product flows, their mode and the flows of its published and deprecated plans, without connectors', async () => {
        const { plans } = serveStudio();
        renderPage();

        const props = await loadedStudio();
        expect(props.apiType).toBe('PROXY');
        expect(props.commonFlows).toEqual([PRODUCT_FLOW]);
        expect(props.flowExecution).toEqual({ mode: 'BEST_MATCH', matchRequired: true });
        expect(props.plans).toEqual([{ id: 'plan-1', name: 'Gold', flows: [PLAN_FLOW] }]);
        expect(props.entrypointsInfo).toEqual([]);
        expect(props.endpointsInfo).toEqual([]);
        expect(new URL(plans.lastCall!.url).searchParams.get('statuses')).toBe('PUBLISHED,DEPRECATED');
    });

    it('offers only HTTP proxy request and response policies and PROXY shared policy groups', async () => {
        serveStudio();
        renderPage();

        await waitFor(() => expect(mockStudioProps?.policies).toHaveLength(2));
        expect(mockStudioProps!.policies.map(p => [p.id, p.flowPhaseCompatibility])).toEqual([
            ['rate-limit', { HTTP_PROXY: ['REQUEST'] }],
            ['transform-headers', { HTTP_PROXY: ['REQUEST', 'RESPONSE'] }],
        ]);
        await waitFor(() => expect(mockStudioProps!.sharedPolicyGroups.map(g => g.id)).toEqual(['spg-proxy']));
    });

    it('is editable with the product definition update permission', async () => {
        serveStudio({ definition: 'RU' });
        renderPage();

        expect((await loadedStudio()).readOnly).toBe(false);
    });

    it('is read-only without the product definition update permission', async () => {
        serveStudio({ definition: 'R' });
        renderPage();

        expect((await loadedStudio()).readOnly).toBe(true);
    });

    it('saves the product flows then the changed plans, and refetches without deploying', async () => {
        const { flows } = serveStudio();
        const putFlows = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
        const putPlanFlows = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, {});
        const deploy = trackHandler('post', `${PRODUCT_PATH}/deployments`, {});
        renderPage();
        const props = await loadedStudio();
        const flowsFetches = flows.callCount;

        await act(() => props.onSave(SAVE_BOTH));

        expect(putFlows.lastCall?.body).toEqual({
            flows: [{ ...PRODUCT_FLOW, enabled: false }],
            flowExecution: { mode: 'BEST_MATCH', matchRequired: true },
        });
        expect(putPlanFlows.lastCall?.body).toEqual([{ planId: 'plan-1', flows: [] }]);
        expect(deploy.callCount).toBe(0);
        await waitFor(() => expect(flows.callCount).toBeGreaterThan(flowsFetches));
    });

    it('sends only the flow mode change with the current flows', async () => {
        serveStudio();
        const putFlows = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
        const putPlanFlows = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, {});
        renderPage();
        const props = await loadedStudio();

        await act(() => props.onSave({ flowExecution: { mode: 'DEFAULT', matchRequired: false } }));

        expect(putFlows.lastCall?.body).toEqual({ flows: [PRODUCT_FLOW], flowExecution: { mode: 'DEFAULT', matchRequired: false } });
        expect(putPlanFlows.callCount).toBe(0);
    });

    it('shows an error and refetches when the plan flows fail to save after the product flows', async () => {
        const { flows, plans } = serveStudio();
        const putFlows = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
        trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, { message: 'Plan is closed' }, 400);
        renderPage();
        const props = await loadedStudio();
        const flowsFetches = flows.callCount;
        const plansFetches = plans.callCount;

        await act(async () => {
            await expect(props.onSave(SAVE_BOTH)).rejects.toBeDefined();
        });

        expect(putFlows.callCount).toBe(1);
        expect(notify.error).toHaveBeenCalledWith(expect.anything(), 'Failed to save the API product flows.');
        await waitFor(() => {
            expect(flows.callCount).toBeGreaterThan(flowsFetches);
            expect(plans.callCount).toBeGreaterThan(plansFetches);
        });
    });

    it('refuses a plan flow change without the plan update permission, before writing anything', async () => {
        serveStudio({ definition: 'RU', plan: 'R' });
        const putFlows = trackHandler('put', `${PRODUCT_PATH}/flows`, {});
        const putPlanFlows = trackHandler('put', `${PRODUCT_PATH}/plans/_flows`, {});
        renderPage();
        const props = await loadedStudio();

        await act(async () => {
            await expect(props.onSave(SAVE_BOTH)).rejects.toThrow('You do not have permission to update plan flows.');
        });

        expect(putFlows.callCount).toBe(0);
        expect(putPlanFlows.callCount).toBe(0);
    });
});
