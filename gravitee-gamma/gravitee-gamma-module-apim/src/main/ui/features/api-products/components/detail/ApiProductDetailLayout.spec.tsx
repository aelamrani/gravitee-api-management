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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { DependencyList, EffectCallback, ReactNode } from 'react';
import { useSyncExternalStore } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiProductDetailLayout } from './ApiProductDetailLayout';
import { resetApimClientForTests } from '../../../../shared/api/apimClient';
import { TEST_V2_BASE } from '../../../../testing/factories';
import { settleOutstandingRequests, trackHandler } from '../../../../testing/helpers';
import { server } from '../../../../testing/server';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(),
}));

// The host shell renders the layout's banner; this test stands in for it with a slot fed by useLayoutConfig.
const mockBannerStore: { banner: ReactNode; listeners: Set<() => void> } = { banner: null, listeners: new Set() };

jest.mock('@gravitee/graphene-core', () => {
    const { useEffect } = jest.requireActual<{ useEffect: (effect: EffectCallback, deps?: DependencyList) => void }>('react');
    return {
        ...jest.requireActual<object>('@gravitee/graphene-core'),
        useLayoutConfig: (config: { banner?: ReactNode }, deps: unknown[] = []) => {
            useEffect(() => {
                mockBannerStore.banner = config.banner ?? null;
                mockBannerStore.listeners.forEach(listener => listener());
                // eslint-disable-next-line react-hooks/exhaustive-deps
            }, deps);
        },
    };
});

function BannerSlot() {
    const banner = useSyncExternalStore(
        listener => {
            mockBannerStore.listeners.add(listener);
            return () => mockBannerStore.listeners.delete(listener);
        },
        () => mockBannerStore.banner,
    );
    return <>{banner}</>;
}

const PRODUCT_PATH = `${TEST_V2_BASE}/api-products/product-1`;

function serveProduct(deploymentState: 'DEPLOYED' | 'NEED_REDEPLOY') {
    server.use(
        http.get(PRODUCT_PATH, () => HttpResponse.json({ id: 'product-1', name: 'Payments', version: '1.0', apiIds: [], deploymentState })),
    );
}

function servePermissions(definition: string) {
    return trackHandler('get', `${PRODUCT_PATH}/members/permissions`, { DEFINITION: definition });
}

function renderLayout() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/api-products/product-1/overview']}>
                <BannerSlot />
                <Routes>
                    <Route path="/api-products/:productId" element={<ApiProductDetailLayout />}>
                        <Route path="overview" element={<div data-testid="overview-page" />} />
                    </Route>
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

describe('ApiProductDetailLayout deploy banner', () => {
    beforeEach(() => {
        resetApimClientForTests();
        mockBannerStore.banner = null;
        (useEnvironment as jest.Mock).mockReturnValue({ id: 'DEFAULT', hrids: ['DEFAULT'] });
    });

    afterEach(() => jest.clearAllMocks());

    it('shows the banner when the product needs a redeploy and the user can update its definition', async () => {
        serveProduct('NEED_REDEPLOY');
        servePermissions('RU');
        renderLayout();

        expect(await screen.findByText('This API product is out of sync')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Deploy API product' })).toBeInTheDocument();
    });

    it('deploys the product on confirm, refetches it and hides the banner', async () => {
        serveProduct('NEED_REDEPLOY');
        servePermissions('RU');
        const deploy = trackHandler('post', `${PRODUCT_PATH}/deployments`, { id: 'product-1' }, 202);
        renderLayout();

        fireEvent.click(await screen.findByRole('button', { name: 'Deploy API product' }));
        expect(await screen.findByText('Deploy your API product')).toBeInTheDocument();
        expect(screen.queryByLabelText(/deployment label/i)).not.toBeInTheDocument();

        serveProduct('DEPLOYED');
        fireEvent.click(screen.getByRole('button', { name: 'Deploy' }));

        await waitFor(() => expect(deploy.callCount).toBe(1));
        expect(deploy.lastCall?.body).toEqual({});
        await waitFor(() => expect(screen.queryByText('This API product is out of sync')).not.toBeInTheDocument());
    });

    it('shows no banner without the product definition update permission', async () => {
        serveProduct('NEED_REDEPLOY');
        const permissions = servePermissions('R');
        renderLayout();

        await waitFor(() => expect(permissions.callCount).toBeGreaterThan(0));
        await settleOutstandingRequests();
        expect(screen.queryByText('This API product is out of sync')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Deploy API product' })).not.toBeInTheDocument();
    });

    it('shows no banner when the product is deployed', async () => {
        serveProduct('DEPLOYED');
        const permissions = servePermissions('RU');
        renderLayout();

        await waitFor(() => expect(permissions.callCount).toBeGreaterThan(0));
        await settleOutstandingRequests();
        expect(screen.queryByText('This API product is out of sync')).not.toBeInTheDocument();
    });
});
