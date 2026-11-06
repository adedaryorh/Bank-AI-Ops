import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { PageEvent } from '@angular/material/paginator';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { AdminUser, ProblemDetails } from '../../core/api/api.models';
import { AdminUsersService } from '../../core/api/admin-users.service';
import { problemFromError } from '../../core/api/problem-details';
import { LanguageService } from '../../core/i18n/language.service';
import { AppIcon } from '../../shared/icons/app-icon';

type LoadState = 'loading' | 'ready' | 'error';

interface ErrorInfo {
  readonly detail: string | null;
  readonly title: string | null;
  readonly correlationId: string | null;
  readonly status: number | null;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    TranslocoPipe,
    AppIcon,
  ],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsersPage {
  private readonly usersService = inject(AdminUsersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly language = inject(LanguageService);
  private readonly transloco = inject(TranslocoService);

  protected readonly displayedColumns: string[] = [
    'displayName',
    'email',
    'department',
    'roles',
    'createdAt',
    'lastSeenAt',
  ];

  protected readonly state = signal<LoadState>('loading');
  protected readonly users = signal<readonly AdminUser[]>([]);
  protected readonly page = signal(0);
  protected readonly size = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly error = signal<ErrorInfo | null>(null);

  protected readonly isForbidden = computed(() => this.error()?.status === 403);

  constructor() {
    this.load();
  }

  protected load(page = this.page(), size = this.size()): void {
    this.state.set('loading');
    this.error.set(null);
    this.page.set(page);
    this.size.set(size);

    this.usersService
      .list(page, size)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.users.set(response.content);
          this.page.set(response.page);
          this.size.set(response.size);
          this.totalElements.set(response.totalElements);
          this.totalPages.set(response.totalPages);
          this.state.set('ready');
        },
        error: (error: unknown) => {
          this.users.set([]);
          this.error.set(this.toErrorInfo(error));
          this.state.set('error');
        },
      });
  }

  protected onPage(event: PageEvent): void {
    const nextPage = event.pageSize !== this.size() ? 0 : event.pageIndex;
    this.load(nextPage, event.pageSize);
  }

  protected formatDateTime(value: string | null): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    const locale = this.language.activeLang() === 'de' ? 'de-AT' : 'en-GB';
    return new Intl.DateTimeFormat(locale, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  protected errorMessage(): string {
    const error = this.error();
    if (error?.detail) {
      return error.detail;
    }
    if (error?.title) {
      return error.title;
    }
    this.language.activeLang();
    return this.transloco.translate(this.isForbidden() ? 'errors.forbidden' : 'errors.unknown');
  }

  private toErrorInfo(error: unknown): ErrorInfo {
    const problem: ProblemDetails | null = problemFromError(error);
    return {
      detail: problem?.detail ?? null,
      title: problem?.title ?? null,
      correlationId: problem?.correlationId ?? null,
      status: problem?.status ?? (error instanceof HttpErrorResponse ? error.status : null),
    };
  }
}
