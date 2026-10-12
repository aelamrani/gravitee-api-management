/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Rule } from './rule.metrics';

import { Scope } from '../alert';

describe('Rule.findCategoriesByScope', () => {
  it('offers the API metrics and Health-check groups at API scope', () => {
    expect(Rule.findCategoriesByScope(Scope.API, false)).toEqual(['API metrics', 'Health-check']);
  });

  it('offers the Node, API metrics and Health-check groups at environment scope', () => {
    expect(Rule.findCategoriesByScope(Scope.ENVIRONMENT, false)).toEqual(['Node', 'API metrics', 'Health-check']);
  });

  it('hides the Node group at environment scope when cloud hosted', () => {
    expect(Rule.findCategoriesByScope(Scope.ENVIRONMENT, true)).toEqual(['API metrics', 'Health-check']);
  });

  it.each([
    [Scope.ENVIRONMENT, false],
    [Scope.ENVIRONMENT, true],
    [Scope.API, false],
    [Scope.API, true],
    [Scope.APPLICATION, false],
  ])('offers a group for every rule available at scope %s (cloud: %s)', (scope: Scope, cloudEnabled: boolean) => {
    const groups = Rule.findCategoriesByScope(scope, cloudEnabled);
    const ruleCategories = Rule.findByScope(scope, cloudEnabled).map(rule => rule.category);

    expect(groups).toEqual(expect.arrayContaining(ruleCategories));
  });
});
