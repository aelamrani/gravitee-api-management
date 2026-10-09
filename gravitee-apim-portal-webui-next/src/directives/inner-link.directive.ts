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
import { Directive, HostListener } from '@angular/core';
import { Router } from '@angular/router';

// Any URL scheme (`https:`, `mailto:`, `tel:`…) or protocol-relative `//host` is not an app route.
const HAS_SCHEME_OR_HOST = /^([a-z][a-z0-9+.-]*:|\/\/)/i;

@Directive({
  selector: '[appInnerLink]',
  standalone: true,
})
export class InnerLinkDirective {
  constructor(private router: Router) {}

  @HostListener('click', ['$event'])
  public onClick(e: MouseEvent) {
    // Hosts rendered with ShadowDom retarget `e.target` to the host element: the real target is first in the composed path.
    const target = (e.composedPath()[0] ?? e.target) as HTMLElement | null;
    const anchor = target?.closest?.('a');
    const href = anchor?.getAttribute('href');
    if (!anchor || !href || HAS_SCHEME_OR_HOST.test(href)) {
      return;
    }
    // Let the browser honor `target` and modifier keys (open in a new tab/window).
    if (e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || (anchor.target && anchor.target !== '_self')) {
      return;
    }

    e.preventDefault();
    if (href.startsWith('#')) {
      this.scrollToAnchor(anchor, href.substring(1));
    } else {
      this.router.navigateByUrl(href);
    }
  }

  // The ids live in the same root (document or shadow root) as the link, and the <base href> would otherwise resolve `#id` to the homepage.
  private scrollToAnchor(anchor: HTMLElement, id: string) {
    const root = anchor.getRootNode() as Document | ShadowRoot;
    root.getElementById?.(decodeURIComponent(id))?.scrollIntoView();
    // preventDefault() keeps the fragment out of the address bar: put it back so the section can be copied or shared.
    history.replaceState(history.state, '', `${location.pathname}${location.search}#${id}`);
  }
}
