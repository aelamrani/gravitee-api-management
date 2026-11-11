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
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApiProductSidebarNav } from './ApiProductSidebarNav';

function renderNav(canReadDefinition: boolean) {
    return render(
        <MemoryRouter>
            <ApiProductSidebarNav basePath="/api-products/product-1" canReadDefinition={canReadDefinition} />
        </MemoryRouter>,
    );
}

describe('ApiProductSidebarNav', () => {
    it('lists Policy Studio right after Plans with the product definition read permission', () => {
        renderNav(true);

        const labels = screen.getAllByRole('link').map(link => link.textContent);
        expect(labels.indexOf('Policy Studio')).toBe(labels.indexOf('Plans') + 1);
        expect(screen.getByRole('link', { name: 'Policy Studio' })).toHaveAttribute('href', '/api-products/product-1/policy-studio');
    });

    it('hides Policy Studio without the product definition read permission', () => {
        renderNav(false);

        expect(screen.getByRole('link', { name: 'Plans' })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: 'Policy Studio' })).not.toBeInTheDocument();
    });
});
