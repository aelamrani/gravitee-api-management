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
import { AfterViewInit, Directive, ElementRef, HostListener, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';

@Directive({
  selector: '[appInnerLink]',
  standalone: true,
})
export class InnerLinkDirective implements AfterViewInit, OnDestroy {
  private fragmentObserver?: MutationObserver;

  constructor(
    private readonly router: Router,
    private readonly location: Location,
    private readonly elementRef: ElementRef<HTMLElement>,
  ) {}

  @HostListener('click', ['$event'])
  public onClick(e: PointerEvent) {
    // The rendered Markdown may live in a shadow root (gmd-viewer), where e.target is retargeted to the host.
    const anchor = e.composedPath().find((node): node is HTMLAnchorElement => (node as Element).tagName === 'A');
    const href = anchor?.getAttribute('href');
    if (!anchor || !href) {
      return;
    }

    if (href.startsWith('#')) {
      e.preventDefault();
      if (this.scrollToSection(this.decodeFragment(href.substring(1)))) {
        this.location.go(this.location.path() + href);
      }
      return;
    }

    if (anchor === e.target && !href.startsWith('https:') && !href.startsWith('http:')) {
      e.preventDefault();
      this.router.navigateByUrl(href);
    }
  }

  ngAfterViewInit(): void {
    const fragment = this.router.parseUrl(this.router.url).fragment;
    if (!fragment || this.scrollToSection(fragment)) {
      return;
    }

    // The Markdown is usually rendered after the view is initialized, once the page content has loaded.
    this.fragmentObserver = new MutationObserver(() => {
      if (this.scrollToSection(fragment)) {
        this.fragmentObserver?.disconnect();
      }
    });
    const host = this.elementRef.nativeElement;
    for (const root of [host, host.shadowRoot]) {
      if (root) {
        this.fragmentObserver.observe(root, { childList: true, subtree: true });
      }
    }
  }

  ngOnDestroy(): void {
    this.fragmentObserver?.disconnect();
  }

  private scrollToSection(id: string): boolean {
    const section = this.findSection(id);
    section?.scrollIntoView();
    return !!section;
  }

  private findSection(id: string): HTMLElement | null {
    const host = this.elementRef.nativeElement;
    const shadowRoot = host.shadowRoot;
    const candidates = [shadowRoot?.getElementById(id), (host.getRootNode() as Document | ShadowRoot).getElementById(id)];
    return candidates.find(element => !!element && (host.contains(element) || !!shadowRoot?.contains(element))) ?? null;
  }

  private decodeFragment(fragment: string): string {
    try {
      return decodeURIComponent(fragment);
    } catch {
      return fragment;
    }
  }
}
