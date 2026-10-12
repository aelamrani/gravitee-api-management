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
import { Component, signal, ViewEncapsulation } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { InnerLinkDirective } from './inner-link.directive';

@Component({
  standalone: true,
  imports: [InnerLinkDirective],
  template: `<div appInnerLink>
    <a id="link" href="#section-two"><span id="link-label">Go to Section Two</span></a>
    <a id="missing-link" href="#unknown-section">Go nowhere</a>
    <a id="route-link" href="/catalog">Catalog</a>
    <h2 id="section-two">Section Two</h2>
  </div>`,
})
class LightDomHostComponent {}

@Component({
  selector: 'app-shadow-content',
  standalone: true,
  encapsulation: ViewEncapsulation.ShadowDom,
  template: `<a id="link" href="#section-two">Go to Section Two</a>
    <h2 id="section-two">Section Two</h2>`,
})
class ShadowContentComponent {}

@Component({
  standalone: true,
  imports: [InnerLinkDirective, ShadowContentComponent],
  template: `<app-shadow-content appInnerLink />`,
})
class ShadowDomHostComponent {}

@Component({
  standalone: true,
  imports: [InnerLinkDirective],
  template: `<div appInnerLink>
    @if (rendered()) {
      <h2 id="section-two">Section Two</h2>
    }
  </div>`,
})
class LateContentHostComponent {
  rendered = signal(false);
}

describe('InnerLinkDirective', () => {
  let router: Router;
  let location: Location;
  let scrollIntoView: jest.Mock;
  const originalScrollIntoView = HTMLElement.prototype.scrollIntoView;

  beforeEach(() => {
    scrollIntoView = jest.fn();
    HTMLElement.prototype.scrollIntoView = scrollIntoView;

    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideLocationMocks()],
    });
    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
  });

  afterEach(() => {
    HTMLElement.prototype.scrollIntoView = originalScrollIntoView;
  });

  function render<T>(component: new (...args: unknown[]) => T): ComponentFixture<T> {
    const fixture = TestBed.createComponent(component);
    fixture.detectChanges();
    return fixture;
  }

  describe('on click', () => {
    let navigateByUrl: jest.SpyInstance;

    beforeEach(() => {
      location.go('/docs?page=1');
      navigateByUrl = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    });

    it('should scroll to the heading and not navigate away when clicking a fragment link', () => {
      const fixture = render(LightDomHostComponent);
      const root: HTMLElement = fixture.nativeElement;

      root.querySelector<HTMLElement>('#link')!.click();

      expect(navigateByUrl).not.toHaveBeenCalled();
      expect(scrollIntoView.mock.contexts).toEqual([root.querySelector('#section-two')]);
      expect(location.path(true)).toEqual('/docs?page=1#section-two');
    });

    it('should scroll to the heading when clicking an element inside a fragment link', () => {
      const fixture = render(LightDomHostComponent);
      const root: HTMLElement = fixture.nativeElement;

      root.querySelector<HTMLElement>('#link-label')!.click();

      expect(navigateByUrl).not.toHaveBeenCalled();
      expect(scrollIntoView.mock.contexts).toEqual([root.querySelector('#section-two')]);
    });

    it('should stay on the page when the fragment matches no element', () => {
      const fixture = render(LightDomHostComponent);
      const root: HTMLElement = fixture.nativeElement;

      const click = new MouseEvent('click', { bubbles: true, cancelable: true });
      root.querySelector<HTMLElement>('#missing-link')!.dispatchEvent(click);

      expect(click.defaultPrevented).toBe(true);
      expect(navigateByUrl).not.toHaveBeenCalled();
      expect(scrollIntoView).not.toHaveBeenCalled();
      expect(location.path(true)).toEqual('/docs?page=1');
    });

    it('should scroll to the heading of a fragment link inside a Shadow DOM', () => {
      const fixture = render(ShadowDomHostComponent);
      const shadowRoot = fixture.nativeElement.querySelector('app-shadow-content').shadowRoot as ShadowRoot;

      shadowRoot.querySelector<HTMLElement>('#link')!.click();

      expect(navigateByUrl).not.toHaveBeenCalled();
      expect(scrollIntoView.mock.contexts).toEqual([shadowRoot.querySelector('#section-two')]);
      expect(location.path(true)).toEqual('/docs?page=1#section-two');
    });

    it('should still route internal links', () => {
      const fixture = render(LightDomHostComponent);

      fixture.nativeElement.querySelector('#route-link').click();

      expect(navigateByUrl).toHaveBeenCalledWith('/catalog');
      expect(scrollIntoView).not.toHaveBeenCalled();
    });
  });

  describe('on load', () => {
    it('should scroll to the heading named by the URL fragment', async () => {
      await router.navigateByUrl('/#section-two');

      const fixture = render(LightDomHostComponent);

      expect(scrollIntoView.mock.contexts).toEqual([fixture.nativeElement.querySelector('#section-two')]);
    });

    it('should scroll to the heading named by the URL fragment once the content is rendered', async () => {
      await router.navigateByUrl('/#section-two');
      const fixture = render(LateContentHostComponent);
      expect(scrollIntoView).not.toHaveBeenCalled();

      fixture.componentInstance.rendered.set(true);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(scrollIntoView.mock.contexts).toEqual([fixture.nativeElement.querySelector('#section-two')]);
    });

    it('should not scroll to late content once the URL no longer carries the fragment', async () => {
      await router.navigateByUrl('/#section-two');
      const fixture = render(LateContentHostComponent);

      await router.navigateByUrl('/');
      fixture.componentInstance.rendered.set(true);
      fixture.detectChanges();
      await fixture.whenStable();

      expect(scrollIntoView).not.toHaveBeenCalled();
    });

    it('should not scroll when the URL has no fragment', async () => {
      await router.navigateByUrl('/');

      render(LightDomHostComponent);

      expect(scrollIntoView).not.toHaveBeenCalled();
    });
  });
});
