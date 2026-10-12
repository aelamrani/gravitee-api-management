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
    private router: Router,
    private location: Location,
    private elementRef: ElementRef<HTMLElement>,
  ) {}

  ngAfterViewInit(): void {
    const fragment = this.router.parseUrl(this.router.url).fragment;
    if (!fragment || this.scrollToId(this.contentRoot(), fragment)) {
      return;
    }
    // The content (innerHTML binding or gmd-viewer rendering) can arrive after the view is ready.
    this.fragmentObserver = new MutationObserver(() => {
      if (this.scrollToId(this.contentRoot(), fragment)) {
        this.fragmentObserver?.disconnect();
      }
    });
    this.fragmentObserver.observe(this.elementRef.nativeElement.shadowRoot ?? this.elementRef.nativeElement, {
      childList: true,
      subtree: true,
    });
  }

  ngOnDestroy(): void {
    this.fragmentObserver?.disconnect();
  }

  @HostListener('click', ['$event'])
  public onClick(e: PointerEvent) {
    const anchor = this.clickedAnchor(e);
    const anchorHref = anchor?.getAttribute('href');
    if (anchor && anchorHref?.startsWith('#')) {
      // Routing '#id' resolves against the base href and lands on the homepage: stay on the page instead.
      e.preventDefault();
      if (this.scrollToId(anchor.getRootNode(), anchorHref.substring(1))) {
        this.location.go(this.location.path() + anchorHref);
      }
      return;
    }

    if (e.target) {
      const target: HTMLLinkElement = e.target as HTMLLinkElement;

      const href = target.getAttribute('href');
      if (target.tagName === 'A' && href) {
        if (href && !href.startsWith('https:') && !href.startsWith('http:')) {
          e.preventDefault();
          this.router.navigateByUrl(href);
        }
      }
    }
  }

  // composedPath reaches into the Shadow DOM of gmd-viewer, where e.target is retargeted to the host.
  private clickedAnchor(e: Event): HTMLAnchorElement | undefined {
    const host = this.elementRef.nativeElement;
    for (const node of e.composedPath()) {
      if (node === host) {
        return undefined;
      }
      if (node instanceof HTMLAnchorElement) {
        return node;
      }
    }
    return undefined;
  }

  private contentRoot(): Node {
    return this.elementRef.nativeElement.shadowRoot ?? this.elementRef.nativeElement.getRootNode();
  }

  private scrollToId(root: Node, id: string): boolean {
    if (!id || !(root instanceof Document || root instanceof ShadowRoot)) {
      return false;
    }
    const element = root.getElementById(id) ?? root.getElementById(safeDecode(id));
    element?.scrollIntoView();
    return !!element;
  }
}

function safeDecode(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}
