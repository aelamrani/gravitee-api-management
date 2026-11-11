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
import {
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
} from '@gravitee/graphene-core';
import { useId, useState } from 'react';

/** Classic console caps the deployment label at 32 characters. */
const DEPLOYMENT_LABEL_MAX_LENGTH = 32;

interface DeployConfirmDialogProps {
    open: boolean;
    title: string;
    isPending: boolean;
    /** The API Product deployment endpoint takes no label, so its dialog only confirms. */
    withLabel?: boolean;
    onConfirm: (label: string) => void;
    onOpenChange: (open: boolean) => void;
}

/** Confirms a deployment, optionally collecting a deployment label — mirrors the classic console deploy dialog. */
export function DeployConfirmDialog({ open, title, isPending, withLabel = true, onConfirm, onOpenChange }: DeployConfirmDialogProps) {
    const labelId = useId();
    const [label, setLabel] = useState('');

    const handleConfirm = () => onConfirm(label.trim());

    return (
        <Dialog
            open={open}
            onOpenChange={next => {
                if (!next) setLabel('');
                onOpenChange(next);
            }}
        >
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>
                        {withLabel ? 'Provide a label to identify your deployment.' : 'Push your latest changes to the gateway.'}
                    </DialogDescription>
                </DialogHeader>
                {withLabel ? (
                    <div className="space-y-2">
                        <Label htmlFor={labelId}>Deployment label (optional)</Label>
                        <Input
                            id={labelId}
                            maxLength={DEPLOYMENT_LABEL_MAX_LENGTH}
                            placeholder="e.g. hotfix-cors, v2.1-release"
                            value={label}
                            onChange={e => setLabel(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && handleConfirm()}
                        />
                        <p className="text-right text-xs text-muted-foreground">
                            {label.length}/{DEPLOYMENT_LABEL_MAX_LENGTH}
                        </p>
                    </div>
                ) : null}
                <DialogFooter>
                    <DialogClose asChild>
                        <Button variant="outline" size="sm">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button size="sm" onClick={handleConfirm} disabled={isPending}>
                        {isPending ? 'Deploying…' : 'Deploy'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
