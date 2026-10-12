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
import { Component, ViewEncapsulation } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DefaultUrlSerializer, Router } from '@angular/router';

import { InnerLinkDirective } from './inner-link.directive';

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
  template: `<div appInnerLink>
      <a id="link" href="#section-two">Go to Section Two</a>
      <a id="missing-link" href="#missing">Missing</a>
      <a id="route-link" href="/catalog">Catalog</a>
      <h2 id="section-two">Section Two</h2>
    </div>
    <app-shadow-content appInnerLink />`,
})
class HostComponent {}

describe('InnerLinkDirective', () => {
  let fixture: ComponentFixture<HostComponent>;
  let router: { url: string; navigateByUrl: jest.Mock; parseUrl: Router['parseUrl'] };
  let location: { path: jest.Mock; go: jest.Mock };
  let scrollIntoView: jest.SpyInstance;

  async function init(url = '/catalog/api/1/documentation') {
    router = { url, navigateByUrl: jest.fn().mockResolvedValue(true), parseUrl: u => new DefaultUrlSerializer().parse(u) };
    location = { path: jest.fn().mockReturnValue('/catalog/api/1/documentation'), go: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: Location, useValue: location },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  }

  beforeEach(() => {
    scrollIntoView = jest.spyOn(HTMLElement.prototype, 'scrollIntoView').mockReturnValue();
  });

  afterEach(() => {
    scrollIntoView.mockRestore();
  });

  function heading(root: ParentNode = fixture.nativeElement): HTMLElement {
    return root.querySelector('#section-two') as HTMLElement;
  }

  function shadowRoot(): ShadowRoot {
    return (fixture.nativeElement.querySelector('app-shadow-content') as HTMLElement).shadowRoot as ShadowRoot;
  }

  it('should scroll to the heading and not navigate away when clicking a fragment link', async () => {
    await init();
    const link = fixture.nativeElement.querySelector('#link') as HTMLAnchorElement;
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });

    link.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(scrollIntoView.mock.contexts).toEqual([heading()]);
    expect(location.go).toHaveBeenCalledWith('/catalog/api/1/documentation#section-two');
  });

  it('should scroll to the heading of a fragment link rendered in a Shadow DOM', async () => {
    await init();
    const link = shadowRoot().querySelector('#link') as HTMLAnchorElement;
    const event = new MouseEvent('click', { bubbles: true, cancelable: true, composed: true });

    link.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(scrollIntoView.mock.contexts).toEqual([heading(shadowRoot())]);
  });

  it('should stay on the page when the fragment link targets no element', async () => {
    await init();
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });

    fixture.nativeElement.querySelector('#missing-link').dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(location.go).not.toHaveBeenCalled();
    expect(scrollIntoView).not.toHaveBeenCalled();
  });

  it('should stay on the page when the fragment link is not valid URI encoding', async () => {
    await init();
    const link = fixture.nativeElement.querySelector('#missing-link') as HTMLAnchorElement;
    link.setAttribute('href', '#%E0%A4%A');
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });

    expect(() => link.dispatchEvent(event)).not.toThrow();
    expect(event.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should navigate with the router when clicking an internal route link', async () => {
    await init();
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });

    fixture.nativeElement.querySelector('#route-link').dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/catalog');
  });

  it('should scroll to the heading named by the URL fragment on load', async () => {
    await init('/catalog/api/1/documentation#section-two');

    expect(scrollIntoView.mock.contexts).toContain(heading());
    expect(scrollIntoView.mock.contexts).toContain(heading(shadowRoot()));
  });

  it('should scroll to the heading named by the URL fragment once it is rendered', async () => {
    await init('/catalog/api/1/documentation#section-three');
    expect(scrollIntoView).not.toHaveBeenCalled();

    const late = document.createElement('h2');
    late.id = 'section-three';
    fixture.nativeElement.querySelector('[appInnerLink]').appendChild(late);
    await new Promise(resolve => setTimeout(resolve));

    expect(scrollIntoView.mock.contexts).toEqual([late]);
  });
});
