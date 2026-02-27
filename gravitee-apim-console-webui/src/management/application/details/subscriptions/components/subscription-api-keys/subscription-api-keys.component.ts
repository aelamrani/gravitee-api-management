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
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { filter, switchMap } from 'rxjs/operators';
import { BehaviorSubject, EMPTY } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';

import { ApplicationSubscriptionApiKey } from '../../../../../../entities/subscription/ApplicationSubscriptionApiKey';
import {
  ApiPortalSubscriptionRenewDialogComponent,
  ApiPortalSubscriptionRenewDialogData,
  ApiPortalSubscriptionRenewDialogResult,
} from '../../../../../api/subscriptions/components/dialogs/renew/api-portal-subscription-renew-dialog.component';
import {
  ApiPortalSubscriptionExpireApiKeyDialogComponent,
  ApiPortalSubscriptionExpireApiKeyDialogData,
  ApiPortalSubscriptionExpireApiKeyDialogResult,
} from '../../../../../api/subscriptions/components/dialogs/expire-api-key/api-portal-subscription-expire-api-key-dialog.component';

export interface SubscriptionApiKeysRenewEvent {
  customApiKey?: string;
}

export interface SubscriptionApiKeysRevokeEvent {
  apiKeyId: string;
}

export interface SubscriptionApiKeysExpireEvent {
  apiKeyId: string;
  expireAt: Date;
}

export interface SubscriptionApiKeysReactivateEvent {
  apiKeyId: string;
}

@Component({
  selector: 'subscription-api-keys',
  templateUrl: './subscription-api-keys.component.html',
  styleUrls: ['./subscription-api-keys.component.scss'],
})
export class SubscriptionApiKeysComponent implements OnInit, OnChanges {
  @Input() apiKeys: ApplicationSubscriptionApiKey[];
  @Input() canRenew: boolean;
  @Input() canRevoke: boolean;
  @Input() canExpire: boolean;
  @Input() canReactivate: boolean;
  @Input() customApiKeyAllowed: boolean = false;
  @Output() renew = new EventEmitter<SubscriptionApiKeysRenewEvent>();
  @Output() revoke = new EventEmitter<SubscriptionApiKeysRevokeEvent>();
  @Output() expire = new EventEmitter<SubscriptionApiKeysExpireEvent>();
  @Output() reactivate = new EventEmitter<SubscriptionApiKeysReactivateEvent>();

  displayedColumns = ['key', 'createdAt', 'endDate', 'actions'];
  dataSource: MatTableDataSource<ApplicationSubscriptionApiKey>;
  private apiKeys$ = new BehaviorSubject<ApplicationSubscriptionApiKey[]>([]);

  constructor(private readonly dialog: MatDialog) {}

  ngOnInit() {
    this.apiKeys$.subscribe((apiKeys) => {
      this.dataSource = new MatTableDataSource<ApplicationSubscriptionApiKey>(apiKeys);
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiKeys) {
      this.apiKeys$.next(changes.apiKeys.currentValue);
    }
  }

  renewApiKey() {
    this.dialog
      .open<ApiPortalSubscriptionRenewDialogComponent, ApiPortalSubscriptionRenewDialogData, ApiPortalSubscriptionRenewDialogResult>(
        ApiPortalSubscriptionRenewDialogComponent,
        {
          data: {
            customApiKeyAllowed: this.customApiKeyAllowed,
          },
          role: 'alertdialog',
          id: 'renewApiKeyDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result) => result?.confirmed),
      )
      .subscribe((result) => {
        this.renew.emit({ customApiKey: result?.customApiKey });
      });
  }

  revokeApiKey(apiKeyId: string) {
    this.dialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Revoke API Key',
          content: 'Are you sure you want to revoke this API key?',
          confirmButton: 'Revoke',
        },
        role: 'alertdialog',
        id: 'revokeApiKeyDialog',
      })
      .afterClosed()
      .pipe(filter((confirmed) => confirmed))
      .subscribe(() => {
        this.revoke.emit({ apiKeyId });
      });
  }

  expireApiKey(apiKey: ApplicationSubscriptionApiKey) {
    this.dialog
      .open<
        ApiPortalSubscriptionExpireApiKeyDialogComponent,
        ApiPortalSubscriptionExpireApiKeyDialogData,
        ApiPortalSubscriptionExpireApiKeyDialogResult
      >(ApiPortalSubscriptionExpireApiKeyDialogComponent, {
        data: {
          expirationDate: apiKey.expire_at,
        },
        role: 'alertdialog',
        id: 'expireApiKeyDialog',
      })
      .afterClosed()
      .pipe(filter((result) => !!result))
      .subscribe((result) => {
        this.expire.emit({ apiKeyId: apiKey.id, expireAt: result.expireAt });
      });
  }

  reactivateApiKey(apiKeyId: string) {
    this.dialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: 'Reactivate API Key',
          content: 'Are you sure you want to reactivate this API key?',
          confirmButton: 'Reactivate',
        },
        role: 'alertdialog',
        id: 'reactivateApiKeyDialog',
      })
      .afterClosed()
      .pipe(filter((confirmed) => confirmed))
      .subscribe(() => {
        this.reactivate.emit({ apiKeyId });
      });
  }
}
