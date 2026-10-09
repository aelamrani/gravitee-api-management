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
import { Component, ViewEncapsulation } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { InnerLinkDirective } from './inner-link.directive';

@Component({
  selector: 'app-shadow-content',
  template: `
    <div appInnerLink>
      <a id="anchor-link" href="#section-two">Go to Section Two</a>
      <a id="inner-link" href="/catalog/api/1/documentation/2">Doc</a>
      <a id="external-link" href="https://example.com">External</a>
      <a id="mailto-link" href="mailto:support@example.com"><strong id="mailto-child">Mail</strong></a>
      <a id="tel-link" href="tel:+123456789">Call</a>
      <a id="protocol-relative-link" href="//cdn.example.com/file.pdf">CDN</a>
      <a id="blank-link" href="/catalog" target="_blank">Blank</a>
      <h2 id="section-two">Section Two</h2>
    </div>
  `,
  encapsulation: ViewEncapsulation.ShadowDom,
  imports: [InnerLinkDirective],
})
class ShadowContentComponent {}

describe('InnerLinkDirective', () => {
  let fixture: ComponentFixture<ShadowContentComponent>;
  let router: Router;
  let shadowRoot: ShadowRoot;

  beforeEach(() => {
    fixture = TestBed.createComponent(ShadowContentComponent);
    fixture.detectChanges();
    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    shadowRoot = fixture.nativeElement.shadowRoot;
  });

  const click = (id: string, init: MouseEventInit = {}) => {
    const event = new MouseEvent('click', { bubbles: true, composed: true, cancelable: true, ...init });
    shadowRoot.getElementById(id)!.dispatchEvent(event);
    return event;
  };

  it('should scroll to the heading and not navigate when clicking an anchor link', () => {
    const heading = shadowRoot.getElementById('section-two')!;
    heading.scrollIntoView = jest.fn();

    const event = click('anchor-link');

    expect(heading.scrollIntoView).toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(true);
  });

  it('should put the fragment in the URL when clicking an anchor link', () => {
    shadowRoot.getElementById('section-two')!.scrollIntoView = jest.fn();

    click('anchor-link');

    expect(location.hash).toBe('#section-two');
  });

  it('should navigate with the router for an internal link', () => {
    click('inner-link');

    expect(router.navigateByUrl).toHaveBeenCalledWith('/catalog/api/1/documentation/2');
  });

  it('should leave external links to the browser', () => {
    const event = click('external-link');

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });

  it.each(['mailto-link', 'mailto-child', 'tel-link', 'protocol-relative-link'])('should leave %s to the browser', id => {
    const event = click(id);

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });

  it('should leave links with a non-self target to the browser', () => {
    const event = click('blank-link');

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });

  it.each(['ctrlKey', 'metaKey', 'shiftKey', 'altKey'])('should leave a click with %s held to the browser', modifier => {
    const event = click('inner-link', { [modifier]: true });

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });
});
