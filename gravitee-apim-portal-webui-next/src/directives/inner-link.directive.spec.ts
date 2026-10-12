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
import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
import { Component, Type, ViewEncapsulation } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { InnerLinkDirective } from './inner-link.directive';

@Component({
  standalone: true,
  template: '',
})
class EmptyRouteComponent {}

@Component({
  standalone: true,
  imports: [InnerLinkDirective],
  template: `<div appInnerLink>
    <a id="fragment-link" href="#section-two">Go to Section Two</a>
    <a id="missing-fragment-link" href="#unknown">Go nowhere</a>
    <a id="encoded-fragment-link" href="#caf%C3%A9">Go to Café</a>
    <a id="internal-link" href="/catalog">Catalog</a>
    <h2 id="section-two">Section Two</h2>
    <h2 id="café">Café</h2>
  </div>`,
})
class LightDomHostComponent {}

@Component({
  selector: 'app-shadow-markdown',
  standalone: true,
  encapsulation: ViewEncapsulation.ShadowDom,
  template: `<a id="fragment-link" href="#section-two">Go to Section Two</a>
    <h2 id="section-two">Section Two</h2>`,
})
class ShadowMarkdownComponent {}

@Component({
  standalone: true,
  imports: [InnerLinkDirective, ShadowMarkdownComponent],
  template: `<app-shadow-markdown appInnerLink />`,
})
class ShadowDomHostComponent {}

@Component({
  standalone: true,
  imports: [InnerLinkDirective],
  template: `<div appInnerLink>
    @if (loaded) {
      <h2 id="section-two">Section Two</h2>
    }
  </div>`,
})
class LateContentHostComponent {
  loaded = false;
}

describe('InnerLinkDirective', () => {
  let router: Router;
  let location: Location;
  let scrollIntoView: jest.SpyInstance;

  beforeEach(async () => {
    scrollIntoView = jest.spyOn(HTMLElement.prototype, 'scrollIntoView');

    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: '**', component: EmptyRouteComponent }]), provideLocationMocks()],
    });
    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
    await router.navigateByUrl('/documentation');
  });

  afterEach(() => {
    scrollIntoView.mockRestore();
  });

  async function render<T>(component: Type<T>): Promise<ComponentFixture<T>> {
    const fixture = TestBed.createComponent(component);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  function scrolledElementIds(): string[] {
    return scrollIntoView.mock.contexts.map(element => (element as Element).id);
  }

  describe('when clicking a link', () => {
    it('should scroll to the section and stay on the page for a fragment link', async () => {
      const fixture = await render(LightDomHostComponent);
      const navigate = jest.spyOn(router, 'navigateByUrl');

      fixture.nativeElement.querySelector('#fragment-link').click();

      expect(navigate).not.toHaveBeenCalled();
      expect(scrolledElementIds()).toEqual(['section-two']);
      expect(location.path(true)).toEqual('/documentation#section-two');
    });

    it('should scroll to a section whose id is percent-encoded in the link', async () => {
      const fixture = await render(LightDomHostComponent);

      fixture.nativeElement.querySelector('#encoded-fragment-link').click();

      expect(scrolledElementIds()).toEqual(['café']);
    });

    it('should neither navigate nor change the URL for a fragment that is not on the page', async () => {
      const fixture = await render(LightDomHostComponent);
      const navigate = jest.spyOn(router, 'navigateByUrl');

      fixture.nativeElement.querySelector('#missing-fragment-link').click();

      expect(navigate).not.toHaveBeenCalled();
      expect(scrollIntoView).not.toHaveBeenCalled();
      expect(location.path(true)).toEqual('/documentation');
    });

    it('should scroll to the section for a fragment link rendered in a shadow root', async () => {
      const fixture = await render(ShadowDomHostComponent);
      const navigate = jest.spyOn(router, 'navigateByUrl');
      const shadowRoot = fixture.nativeElement.querySelector('app-shadow-markdown').shadowRoot as ShadowRoot;

      (shadowRoot.querySelector('#fragment-link') as HTMLElement).click();

      expect(navigate).not.toHaveBeenCalled();
      expect(scrolledElementIds()).toEqual(['section-two']);
      expect(location.path(true)).toEqual('/documentation#section-two');
    });

    it('should still route internal links through the router', async () => {
      const fixture = await render(LightDomHostComponent);
      const navigate = jest.spyOn(router, 'navigateByUrl');

      fixture.nativeElement.querySelector('#internal-link').click();

      expect(navigate).toHaveBeenCalledWith('/catalog');
    });
  });

  describe('when the page is opened with a fragment in the URL', () => {
    it('should scroll to the section already rendered', async () => {
      await router.navigateByUrl('/documentation#section-two');

      await render(LightDomHostComponent);

      expect(scrolledElementIds()).toEqual(['section-two']);
    });

    it('should scroll to the section once it is rendered', async () => {
      await router.navigateByUrl('/documentation#section-two');
      const fixture = await render(LateContentHostComponent);
      expect(scrollIntoView).not.toHaveBeenCalled();

      fixture.componentInstance.loaded = true;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(scrolledElementIds()).toEqual(['section-two']);
    });

    it('should not scroll when the URL has no fragment', async () => {
      await render(LightDomHostComponent);

      expect(scrollIntoView).not.toHaveBeenCalled();
    });
  });
});
