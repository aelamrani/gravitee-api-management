/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, signal, ViewEncapsulation } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { InnerLinkDirective } from './inner-link.directive';

const SECTIONS_HTML = `
  <a id="go-to-section-two" href="#section-two">Go to Section Two</a>
  <a id="go-to-catalog" href="/catalog">Catalog</a>
  <h2 id="section-one">Section One</h2>
  <h2 id="section-two">Section Two</h2>
`;

@Component({
  selector: 'app-light-dom-content',
  imports: [InnerLinkDirective],
  template: `<div [innerHTML]="content()" appInnerLink></div>`,
})
class LightDomContentComponent {
  content = signal(SECTIONS_HTML);
}

@Component({
  selector: 'app-late-content',
  imports: [InnerLinkDirective],
  template: `<div [innerHTML]="content()" appInnerLink></div>`,
})
class LateContentComponent {
  content = signal('');
}

@Component({
  selector: 'app-shadow-dom-viewer',
  template: `<div [innerHTML]="content"></div>`,
  encapsulation: ViewEncapsulation.ShadowDom,
})
class ShadowDomViewerComponent {
  content = SECTIONS_HTML;
}

@Component({
  selector: 'app-shadow-dom-content',
  imports: [InnerLinkDirective, ShadowDomViewerComponent],
  template: `<app-shadow-dom-viewer appInnerLink />`,
})
class ShadowDomContentComponent {}

@Component({ template: '' })
class CatalogComponent {}

describe('InnerLinkDirective', () => {
  let harness: RouterTestingHarness;
  let router: Router;
  let scrolledElementIds: string[];

  beforeEach(async () => {
    scrolledElementIds = [];
    Element.prototype.scrollIntoView = jest.fn(function (this: Element) {
      scrolledElementIds.push(this.id);
    });

    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'light', component: LightDomContentComponent },
          { path: 'late', component: LateContentComponent },
          { path: 'shadow', component: ShadowDomContentComponent },
          { path: 'catalog', component: CatalogComponent },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  });

  async function flushContentObserver() {
    await new Promise(resolve => setTimeout(resolve));
  }

  describe('anchor link click', () => {
    it('should scroll to the section and keep the current page when the content is in the light DOM', async () => {
      await harness.navigateByUrl('/light?tab=docs');

      harness.routeNativeElement!.querySelector<HTMLAnchorElement>('#go-to-section-two')!.click();
      await harness.fixture.whenStable();

      expect(scrolledElementIds).toEqual(['section-two']);
      expect(router.url).toEqual('/light?tab=docs#section-two');
    });

    it('should scroll to the section and keep the current page when the content is in a shadow root', async () => {
      await harness.navigateByUrl('/shadow');

      const shadowRoot = harness.routeNativeElement!.querySelector('app-shadow-dom-viewer')!.shadowRoot!;
      shadowRoot.querySelector<HTMLAnchorElement>('#go-to-section-two')!.click();
      await harness.fixture.whenStable();

      expect(scrolledElementIds).toEqual(['section-two']);
      expect(router.url).toEqual('/shadow#section-two');
    });

    it('should stay on the current page when the section does not exist', async () => {
      await harness.navigateByUrl('/light');
      const anchor = harness.routeNativeElement!.querySelector<HTMLAnchorElement>('#go-to-section-two')!;
      anchor.setAttribute('href', '#unknown-section');

      anchor.click();
      await harness.fixture.whenStable();

      expect(scrolledElementIds).toEqual([]);
      expect(router.url).toEqual('/light');
    });
  });

  describe('URL with a fragment', () => {
    it('should scroll to the section once the content is rendered', async () => {
      const component = await harness.navigateByUrl('/late#section-two', LateContentComponent);
      await flushContentObserver();
      expect(scrolledElementIds).toEqual([]);

      component.content.set(SECTIONS_HTML);
      harness.detectChanges();
      await flushContentObserver();

      expect(scrolledElementIds).toEqual(['section-two']);
    });

    it('should scroll to the section rendered in a shadow root', async () => {
      await harness.navigateByUrl('/shadow#section-two');
      await flushContentObserver();

      expect(scrolledElementIds).toEqual(['section-two']);
    });
  });

  it('should navigate within the portal on a relative link', async () => {
    await harness.navigateByUrl('/light');

    harness.routeNativeElement!.querySelector<HTMLAnchorElement>('#go-to-catalog')!.click();
    await harness.fixture.whenStable();

    expect(router.url).toEqual('/catalog');
  });
});
