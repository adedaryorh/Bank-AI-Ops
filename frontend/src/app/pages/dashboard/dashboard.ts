import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AuditEntry,
  DashboardMetric,
  DashboardSummary,
  DashboardWidget,
  ProblemDetails,
} from '../../core/api/api.models';
import { DashboardService } from '../../core/api/dashboard.service';
import { problemFromError } from '../../core/api/problem-details';
import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppIcon } from '../../shared/icons/app-icon';
import { MetricsChart } from '../../shared/chart/metrics-chart';

type LoadState = 'loading' | 'ready' | 'error';

interface WidgetGroup {
  readonly module: string;
  readonly widgets: readonly DashboardWidget[];
}

interface ErrorInfo {
  readonly detail: string | null;
  readonly title: string | null;
  readonly correlationId: string | null;
  readonly status: number | null;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    TranslocoPipe,
    AppIcon,
    MetricsChart,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardPage {
  private readonly dashboard = inject(DashboardService);
  private readonly transloco = inject(TranslocoService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly language = inject(LanguageService);
  protected readonly auth = inject(AuthService);

  protected readonly state = signal<LoadState>('loading');
  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly error = signal<ErrorInfo | null>(null);

  protected readonly displayUser = computed(() => this.summary()?.user ?? this.auth.user() ?? null);

  protected readonly availableMetrics = computed(() =>
    (this.summary()?.metrics ?? []).filter(
      (metric): metric is DashboardMetric & { value: number } =>
        metric.available && typeof metric.value === 'number' && Number.isFinite(metric.value),
    ),
  );

  /** A chart needs at least two measured values; otherwise it is hidden. */
  protected readonly chartAvailable = computed(() => this.availableMetrics().length >= 2);

  protected readonly widgetGroups = computed<readonly WidgetGroup[]>(() => {
    const widgets = this.summary()?.widgets ?? [];
    const groups = new Map<string, DashboardWidget[]>();
    for (const widget of widgets) {
      const group = groups.get(widget.module);
      if (group) {
        group.push(widget);
      } else {
        groups.set(widget.module, [widget]);
      }
    }
    return [...groups.entries()].map(([module, groupWidgets]) => ({
      module,
      widgets: groupWidgets,
    }));
  });

  protected readonly isEmpty = computed(() => {
    const summary = this.summary();
    if (!summary) {
      return false;
    }
    return (
      summary.metrics.length === 0 &&
      summary.widgets.length === 0 &&
      summary.recentAudit.length === 0
    );
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.error.set(null);
    this.dashboard
      .getSummary()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (summary) => {
          this.summary.set(summary);
          this.state.set('ready');
        },
        error: (error: unknown) => {
          this.summary.set(null);
          this.error.set(this.toErrorInfo(error));
          this.state.set('error');
        },
      });
  }

  protected generatedAt(): string {
    return this.formatDateTime(this.summary()?.generatedAt);
  }

  protected formatDateTime(value: string | null | undefined): string {
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

  /** Falls back to the raw audit code so no server value is hidden. */
  protected actionLabel(action: string): string {
    this.language.activeLang();
    const key = `audit.actions.${action}`;
    const translated = this.transloco.translate(key);
    return translated === key ? action : translated;
  }

  protected outcomeLabel(outcome: string): string {
    this.language.activeLang();
    const key = `audit.outcomes.${outcome}`;
    const translated = this.transloco.translate(key);
    return translated === key ? outcome : translated;
  }

  protected outcomeClass(outcome: string): string {
    switch (outcome.toUpperCase()) {
      case 'SUCCESS':
        return 'outcome-success';
      case 'FAILURE':
        return 'outcome-failure';
      case 'DENIED':
        return 'outcome-denied';
      default:
        return 'outcome-neutral';
    }
  }

  protected widgetRoute(key: string): string | null {
    if (key === 'identity.users' && this.auth.isAdmin()) {
      return '/admin/users';
    }
    return (
      (
        {
          'operations.cases': '/cases',
          'reviews.queue': '/reviews',
          'compliance.policies': '/policies',
          'aml.alerts': '/aml',
          'audit.trail': '/activity',
        } as Record<string, string>
      )[key] ?? null
    );
  }

  protected auditTrack(index: number, entry: AuditEntry): string {
    return entry.id || String(index);
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
    if (error?.status === 0) {
      return this.transloco.translate('errors.network');
    }
    if (error?.status === 401) {
      return this.transloco.translate('errors.unauthorized');
    }
    if (error?.status === 403) {
      return this.transloco.translate('errors.forbidden');
    }
    if (error?.status === 404) {
      return this.transloco.translate('errors.notFound');
    }
    if (error?.status !== null && error?.status !== undefined && error.status >= 500) {
      return this.transloco.translate('errors.server');
    }
    return this.transloco.translate('errors.unknown');
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
