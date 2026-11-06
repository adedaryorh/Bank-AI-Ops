import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthError, AuthService } from '../../core/auth/auth.service';
import { AppIcon } from '../../shared/icons/app-icon';

type CallbackState = 'loading' | 'error';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    TranslocoPipe,
    AppIcon,
  ],
  template: `
    <main class="callback-page">
      <mat-card appearance="outlined" class="callback-card" data-testid="auth-callback">
        @if (state() === 'loading') {
          <mat-spinner diameter="46" />
          <h1>{{ 'callback.title' | transloco }}</h1>
          <p class="body">{{ 'callback.body' | transloco }}</p>
        } @else {
          <div class="icon"><app-icon name="warning" [size]="30" /></div>
          <h1>{{ 'callback.failedTitle' | transloco }}</h1>
          <p class="body">{{ errorMessageKey() | transloco }}</p>
          @if (errorDetail()) {
            <p class="detail">{{ errorDetail() }}</p>
          }
          <div class="actions">
            <button mat-flat-button color="primary" type="button" (click)="retry()">
              <app-icon name="refresh" [size]="18" />
              <span>{{ 'callback.retry' | transloco }}</span>
            </button>
            <a mat-button routerLink="/">{{ 'callback.back' | transloco }}</a>
          </div>
        }
        <p class="notice">{{ 'app.footerNotice' | transloco }}</p>
      </mat-card>
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100vh;
        background: var(--bankops-bg);
        color: var(--bankops-text);
      }

      .callback-page {
        display: grid;
        place-items: center;
        min-height: 100vh;
        padding: 24px;
        box-sizing: border-box;
      }

      .callback-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        max-width: 520px;
        padding: 38px 32px 26px;
        border-radius: 18px;
        box-shadow: none;
      }

      .icon {
        display: grid;
        place-items: center;
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: var(--bankops-danger-bg);
        color: var(--bankops-danger);
      }

      h1 {
        margin: 14px 0 0;
        font-size: 24px;
        font-weight: 760;
        letter-spacing: -0.02em;
      }

      .body {
        margin: 8px 0 0;
        font-size: 14px;
        color: var(--bankops-muted);
        line-height: 1.55;
      }

      .detail {
        margin: 14px 0 0;
        padding: 9px 12px;
        border-radius: 8px;
        background: var(--bankops-surface-alt);
        border: 1px solid var(--bankops-border);
        font-size: 12px;
        color: var(--bankops-muted);
        overflow-wrap: anywhere;
      }

      .actions {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        justify-content: center;
        margin-top: 20px;
      }

      .actions button,
      .actions a {
        display: inline-flex;
        align-items: center;
        gap: 7px;
      }

      .notice {
        margin: 30px 0 0;
        font-size: 10px;
        line-height: 1.5;
        color: var(--bankops-muted);
      }
    `,
  ],
})
export class AuthCallbackPage {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly state = signal<CallbackState>('loading');
  protected readonly errorMessageKey = signal('callback.failedBody');
  protected readonly errorDetail = signal<string | null>(null);

  constructor() {
    void this.handleCallback();
  }

  protected retry(): void {
    void this.auth.startLogin('/');
  }

  private async handleCallback(): Promise<void> {
    const params = this.route.snapshot.queryParamMap;
    const oauthError = params.get('error');

    if (oauthError) {
      this.errorMessageKey.set(
        oauthError === 'login_unavailable' ? 'callback.loginUnavailable' : 'callback.failedBody',
      );
      this.errorDetail.set(params.get('error_description'));
      this.state.set('error');
      return;
    }

    const code = params.get('code');
    const state = params.get('state');
    if (!code || !state) {
      this.errorMessageKey.set('callback.missingParams');
      this.state.set('error');
      return;
    }

    try {
      const returnUrl = await this.auth.completeLogin(code, state);
      await this.router.navigateByUrl(returnUrl, { replaceUrl: true });
    } catch (error: unknown) {
      this.errorMessageKey.set(this.messageKeyFor(error));
      this.errorDetail.set(error instanceof Error ? error.message : null);
      this.state.set('error');
    }
  }

  private messageKeyFor(error: unknown): string {
    if (!(error instanceof AuthError)) {
      return 'callback.failedBody';
    }
    switch (error.code) {
      case 'state_missing':
        return 'callback.stateMissing';
      case 'state_mismatch':
        return 'callback.stateMismatch';
      case 'token_exchange_failed':
        return 'callback.tokenExchangeFailed';
      case 'userinfo_failed':
        return 'callback.userinfoFailed';
      case 'session_ended':
        return 'callback.sessionEnded';
      case 'discovery_failed':
        return 'callback.loginUnavailable';
      default:
        return 'callback.failedBody';
    }
  }
}
