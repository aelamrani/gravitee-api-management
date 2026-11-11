/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Alert, AlertAction, AlertDescription, AlertTitle, Button } from '@gravitee/graphene-core';
import { TriangleAlertIcon } from '@gravitee/graphene-core/icons';

interface DeployBannerProps {
    title: string;
    actionLabel: string;
    onDeploy: () => void;
    isPending: boolean;
}

export function DeployBanner({ title, actionLabel, onDeploy, isPending }: DeployBannerProps) {
    return (
        // A persistent status banner, not an interruption — override Alert's assertive `role="alert"`.
        <Alert variant="warning" role="region" aria-label="Deploy status" className="rounded-none border-0 border-b px-6">
            <TriangleAlertIcon aria-hidden="true" />
            <AlertTitle>{title}</AlertTitle>
            <AlertDescription>Your latest changes are not live yet. Deploy to push them to the gateway.</AlertDescription>
            <AlertAction align="center">
                <Button size="sm" onClick={onDeploy} disabled={isPending}>
                    {isPending ? 'Deploying…' : actionLabel}
                </Button>
            </AlertAction>
        </Alert>
    );
}
