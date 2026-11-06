import { Component, Directive, inject, input, output, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { WorkspaceService } from '../../core/api/workspace.service';
import { AuthService } from '../../core/auth/auth.service';
import { describeError } from '../../core/api/problem-details';
@Component({
  selector: 'app-workspace-status',
  standalone: true,
  imports: [TranslocoPipe, MatProgressBarModule],
  template: `
    @if (busy()) {
      <mat-progress-bar mode="indeterminate" [attr.aria-label]="'work.loading' | transloco" />
    }
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
  `,
  styles: [
    `
      .error {
        padding: 1rem;
        border-left: 4px solid #ba1a1a;
        background: var(--mat-sys-error-container);
        color: var(--mat-sys-on-error-container);
        white-space: pre-wrap;
      }
    `,
  ],
})
export class WorkspaceStatus {
  busy = input(false);
  error = input('');
}
@Component({
  selector: 'app-workspace-pager',
  standalone: true,
  imports: [TranslocoPipe, MatButtonModule],
  template: ` <nav class="pager" [attr.aria-label]="'work.pages' | transloco">
    <button
      mat-stroked-button
      type="button"
      [disabled]="busy() || page() === 0"
      (click)="changed.emit(page() - 1)"
    >
      {{ 'work.previous' | transloco }}
    </button>
    <span>{{ page() + 1 }} / {{ total() > 0 ? total() : 1 }}</span>
    <button
      mat-stroked-button
      type="button"
      [disabled]="busy() || page() + 1 >= total()"
      (click)="changed.emit(page() + 1)"
    >
      {{ 'work.next' | transloco }}
    </button>
  </nav>`,
  styles: [
    `
      .pager {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-top: 1.5rem;
      }
    `,
  ],
})
export class WorkspacePager {
  page = input(0);
  total = input(0);
  busy = input(false);
  changed = output<number>();
}
@Directive()
export abstract class WorkspacePage {
  protected readonly api = inject(WorkspaceService);
  protected readonly auth = inject(AuthService);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected has(...roles: string[]): boolean {
    return roles.some((role) => this.auth.hasRole(role));
  }
  protected async run(action: () => Promise<void>): Promise<void> {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    try {
      await action();
    } catch (error: unknown) {
      this.error.set(describeError(error));
    } finally {
      this.busy.set(false);
    }
  }
}
