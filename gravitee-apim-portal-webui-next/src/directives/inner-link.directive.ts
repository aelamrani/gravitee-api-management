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
import { AfterViewInit, DestroyRef, Directive, ElementRef, HostListener, inject } from '@angular/core';
import { Router } from '@angular/router';

@Directive({
  selector: '[appInnerLink]',
  standalone: true,
})
export class InnerLinkDirective implements AfterViewInit {
  private readonly router = inject(Router);
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);
  private scrolledFragment: string | null = null;

  public ngAfterViewInit(): void {
    this.scrollToUrlFragment();

    // The content is rendered after this directive is initialized, so the URL fragment can only be resolved once it lands.
    const host = this.elementRef.nativeElement;
    const contentObserver = new MutationObserver(() => this.scrollToUrlFragment());
    contentObserver.observe(host.shadowRoot ?? host, { childList: true, subtree: true });
    this.destroyRef.onDestroy(() => contentObserver.disconnect());
  }

  @HostListener('click', ['$event'])
  public onClick(e: PointerEvent) {
    const anchor = this.findClickedAnchor(e);
    const anchorHref = anchor?.getAttribute('href');
    if (anchor && anchorHref?.startsWith('#')) {
      // Routing a bare fragment would resolve it against the base href, i.e. the portal homepage.
      e.preventDefault();
      this.navigateToFragment(anchor, decodeFragment(anchorHref.substring(1)));
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

  private findClickedAnchor(e: Event): HTMLAnchorElement | undefined {
    // composedPath() reaches into shadow roots (e.g. gmd-viewer), where e.target is retargeted to the host.
    const path = e.composedPath();
    const hostIndex = path.indexOf(this.elementRef.nativeElement);
    const pathInsideHost = hostIndex === -1 ? path : path.slice(0, hostIndex);
    return pathInsideHost.find((node): node is HTMLAnchorElement => node instanceof HTMLAnchorElement);
  }

  private navigateToFragment(anchor: HTMLAnchorElement, fragment: string): void {
    const section = findElementById(fragment, [anchor.getRootNode(), ...this.contentRoots()]);
    if (!section) {
      return;
    }
    this.scrolledFragment = fragment;
    section.scrollIntoView();
    this.router.navigate([], { fragment, queryParamsHandling: 'preserve' });
  }

  private scrollToUrlFragment(): void {
    const fragment = this.router.parseUrl(this.router.url).fragment;
    if (!fragment || fragment === this.scrolledFragment) {
      return;
    }
    const section = findElementById(fragment, this.contentRoots());
    if (section) {
      this.scrolledFragment = fragment;
      section.scrollIntoView();
    }
  }

  private contentRoots(): Node[] {
    const host = this.elementRef.nativeElement;
    return host.shadowRoot ? [host.shadowRoot, host.getRootNode()] : [host.getRootNode()];
  }
}

function findElementById(id: string, roots: Node[]): HTMLElement | null {
  for (const root of roots) {
    if (root instanceof Document || root instanceof ShadowRoot) {
      const element = root.getElementById(id);
      if (element) {
        return element;
      }
    }
  }
  return null;
}

function decodeFragment(fragment: string): string {
  try {
    return decodeURIComponent(fragment);
  } catch {
    return fragment;
  }
}
