import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcon } from '../../shared/icons/app-icon';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatCardModule, TranslocoPipe, AppIcon],
  template: `
    <main class="forbidden-page">
      <mat-card appearance="outlined" class="forbidden-card" data-testid="forbidden-page">
        <div class="icon"><app-icon name="lock" [size]="32" /></div>
        <p class="status-code">403</p>
        <h1>{{ 'forbidden.title' | transloco }}</h1>
        <p class="body">{{ 'forbidden.body' | transloco }}</p>
        <p class="usability">{{ 'forbidden.usability' | transloco }}</p>
        <a mat-flat-button routerLink="/">
          <app-icon name="arrowBack" [size]="18" />
          <span>{{ 'forbidden.back' | transloco }}</span>
        </a>
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

      .forbidden-page {
        display: grid;
        place-items: center;
        min-height: 100vh;
        padding: 24px;
        box-sizing: border-box;
      }

      .forbidden-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        max-width: 560px;
        padding: 40px 32px 28px;
        border-radius: 18px;
        box-shadow: none;
      }

      .icon {
        display: grid;
        place-items: center;
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: var(--bankops-warning-bg);
        color: var(--bankops-warning);
      }

      .status-code {
        margin: 14px 0 0;
        font-size: 11px;
        font-weight: 800;
        letter-spacing: 0.16em;
        color: var(--bankops-muted);
      }

      h1 {
        margin: 4px 0 0;
        font-size: 26px;
        font-weight: 780;
        letter-spacing: -0.02em;
      }

      .body {
        margin: 8px 0 0;
        font-size: 14px;
        color: var(--bankops-muted);
      }

      .usability {
        margin: 14px 0 0;
        padding: 10px 14px;
        border-radius: 10px;
        background: var(--bankops-surface-alt);
        border: 1px solid var(--bankops-border);
        font-size: 12px;
        line-height: 1.5;
        color: var(--bankops-muted);
      }

      a {
        margin-top: 20px;
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
export class ForbiddenPage {}
