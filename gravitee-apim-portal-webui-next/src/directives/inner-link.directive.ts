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
    if (!fragment) {
      return;
    }
    // The content is rendered after the view (innerHTML, Shadow DOM), so wait for the target to appear.
    const root = this.contentRoot();
    if (this.scrollToId(root, fragment)) {
      return;
    }
    this.fragmentObserver = new MutationObserver(() => {
      if (this.scrollToId(root, fragment)) {
        this.fragmentObserver?.disconnect();
      }
    });
    this.fragmentObserver.observe(root, { childList: true, subtree: true });
  }

  ngOnDestroy(): void {
    this.fragmentObserver?.disconnect();
  }

  @HostListener('click', ['$event'])
  public onClick(e: PointerEvent) {
    // Events from a Shadow DOM are retargeted to its host: the real clicked element is first in the composed path.
    const target = (e.composedPath?.()[0] ?? e.target) as HTMLElement | null;
    if (!target) {
      return;
    }

    const href = target.getAttribute('href');
    if (target.tagName === 'A' && href) {
      if (href.startsWith('#')) {
        e.preventDefault();
        this.fragmentObserver?.disconnect();
        const id = this.decodeFragment(href.substring(1));
        if (id && this.scrollToId(target.getRootNode() as Document | ShadowRoot, id)) {
          this.location.go(`${this.location.path(false)}#${encodeURIComponent(id)}`);
        }
      } else if (!href.startsWith('https:') && !href.startsWith('http:')) {
        e.preventDefault();
        this.router.navigateByUrl(href);
      }
    }
  }

  private contentRoot(): HTMLElement | ShadowRoot {
    const host = this.elementRef.nativeElement;
    return host.shadowRoot ?? host;
  }

  private decodeFragment(fragment: string): string {
    try {
      return decodeURIComponent(fragment);
    } catch {
      return fragment;
    }
  }

  private scrollToId(root: ParentNode, id: string): boolean {
    const element = Array.from(root.querySelectorAll('[id]')).find(candidate => candidate.id === id);
    element?.scrollIntoView();
    return !!element;
  }
}
