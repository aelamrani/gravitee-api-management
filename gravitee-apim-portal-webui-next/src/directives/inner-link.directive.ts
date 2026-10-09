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
    if (!anchor || !href || href.startsWith('https:') || href.startsWith('http:')) {
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
  }
}
