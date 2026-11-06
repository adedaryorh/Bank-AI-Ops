import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  provideTransloco,
  TranslocoLoader,
  TranslocoService,
  Translation,
} from '@jsverse/transloco';
import { Observable, of } from 'rxjs';
import { throwError } from 'rxjs';
import { DashboardSummary } from '../../core/api/api.models';
import { DashboardService } from '../../core/api/dashboard.service';
import { DashboardPage } from './dashboard';

const TRANSLATIONS = {
  app: { footerNotice: 'Synthetic data only.' },
  dashboard: {
    title: 'Dashboard',
    subtitle: 'Phase 1 overview grounded in backend data.',
    welcome: 'Welcome, {{name}}',
    generatedAt: 'Generated {{date}}',
    noticeTitle: 'Notice',
    metricsTitle: 'Key figures',
    metricsSubtitle: 'Measured values.',
    widgetsTitle: 'Modules',
    widgetsSubtitle: 'Only modules that exist for your role are enabled.',
    recentAuditTitle: 'Recent audit events',
    recentAuditSubtitle: 'Append-only events from the audit log.',
    chartTitle: 'Available metrics',
    chartSubtitle: 'Bar chart drawn only from metrics marked available.',
    chartInsufficient:
      'Not enough measured data for a chart yet. A chart appears once at least two metrics are available.',
    available: 'Available',
    unavailable: 'Not available',
    open: 'Open',
    retry: 'Retry',
    errorTitle: 'The dashboard could not be loaded',
    emptyTitle: 'Nothing to show yet',
    emptyBody: 'The API returned no metrics, widgets or recent audit events.',
    noMetrics: 'No key figures were returned.',
    noWidgets: 'No modules were returned for your role.',
    noAudit: 'No recent audit events.',
    auditActor: 'Actor',
    auditAction: 'Action',
    auditTime: 'Time',
    auditOutcome: 'Outcome',
    valueUnavailable: 'Unavailable',
    notReported: 'Not reported',
    phaseReason: 'Reason from the API',
  },
  errors: {
    genericTitle: 'Something went wrong',
    network: 'The API could not be reached.',
    unauthorized: 'Your session is not authorized for this request.',
    forbidden: 'You do not have permission for this action.',
    notFound: 'The requested resource was not found.',
    server: 'The API reported a server error.',
    problemDetail: 'Problem detail',
    correlationId: 'Correlation ID',
    status: 'HTTP status',
    unknown: 'An unexpected error occurred.',
  },
  audit: {
    actions: { USER_PROVISIONED: 'User provisioned' },
    outcomes: { SUCCESS: 'Success' },
  },
  roles: { OPERATIONS_OFFICER: 'Operations officer' },
};

class DashboardTestLoader implements TranslocoLoader {
  getTranslation(): Observable<Translation> {
    return of(TRANSLATIONS);
  }
}

function makeSummary(overrides: Partial<DashboardSummary> = {}): DashboardSummary {
  return {
    generatedAt: '2026-10-24T15:00:00Z',
    user: {
      id: '11111111-1111-1111-1111-111111111111',
      subject: 'ops@bankops.local',
      email: 'ops@bankops.local',
      displayName: 'Otto Operations',
      department: 'OPS',
      departmentName: 'Banking Operations',
      roles: ['OPERATIONS_OFFICER'],
    },
    metrics: [
      {
        key: 'users.total',
        label: 'Provisioned users',
        value: 6,
        available: true,
        reason: null,
      },
      {
        key: 'operations.cases',
        label: 'Open cases',
        value: null,
        available: false,
        reason: 'Case management arrives in Phase 2.',
      },
    ],
    widgets: [
      {
        key: 'operations.cases',
        label: 'Open cases',
        module: 'Banking Operations',
        available: false,
        reason: 'Case management arrives in Phase 2.',
      },
    ],
    recentAudit: [
      {
        id: '22222222-2222-2222-2222-222222222222',
        occurredAt: '2026-10-24T14:55:00Z',
        action: 'USER_PROVISIONED',
        actorEmail: 'ops@bankops.local',
        outcome: 'SUCCESS',
      },
    ],
    notice: 'Synthetic data only. Decision support, not a production system.',
    ...overrides,
  };
}

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let service: jasmine.SpyObj<DashboardService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<DashboardService>('DashboardService', ['getSummary']);
    service.getSummary.and.returnValue(of(makeSummary()));

    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        { provide: DashboardService, useValue: service },
        ...provideTransloco({
          config: {
            availableLangs: ['en', 'de'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
            prodMode: false,
          },
          loader: DashboardTestLoader,
        }),
      ],
    }).compileComponents();

    const transloco = TestBed.inject(TranslocoService);
    transloco.setTranslation(TRANSLATIONS, 'en');
    transloco.setActiveLang('en');

    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders metrics, the API notice and the unavailable reason', () => {
    const element: HTMLElement = fixture.nativeElement;
    const cards = element.querySelectorAll('[data-testid="metric-card"]');

    expect(cards.length).toBe(2);
    expect(element.textContent).toContain('Provisioned users');
    expect(element.textContent).toContain('6');
    expect(element.textContent).toContain('Not available');
    expect(element.textContent).toContain('Case management arrives in Phase 2.');
    expect(element.querySelector('[data-testid="api-notice"]')?.textContent).toContain(
      'Synthetic data only.',
    );
    expect(element.querySelector('[data-testid="dashboard-user-email"]')?.textContent).toContain(
      'ops@bankops.local',
    );
  });

  it('shows unavailable widgets visibly disabled with their reason', () => {
    const element: HTMLElement = fixture.nativeElement;
    const widget = element.querySelector('[data-testid="widget-card"]') as HTMLElement | null;

    expect(widget).not.toBeNull();
    expect(widget?.classList.contains('unavailable')).toBeTrue();
    expect(widget?.getAttribute('aria-disabled')).toBe('true');
    expect(widget?.textContent).toContain('Open cases');
    expect(widget?.textContent).toContain('Not available');
    expect(widget?.textContent).toContain('Case management arrives in Phase 2.');
    // Unavailable is never rendered as a zero.
    expect(widget?.textContent).not.toContain('0');
  });

  it('does not render a Chart.js chart when fewer than two metrics are available', () => {
    const element: HTMLElement = fixture.nativeElement;
    // Only users.total is available in the fixture; operations.cases is unavailable.
    expect(element.querySelector('app-metrics-chart')).toBeNull();
    expect(element.querySelector('[data-testid="chart-note"]')).not.toBeNull();
  });

  it('renders a Chart.js canvas when at least two metrics are available', async () => {
    service.getSummary.and.returnValue(
      of(
        makeSummary({
          metrics: [
            { key: 'users.total', label: 'Provisioned users', value: 6, available: true, reason: null },
            { key: 'departments.total', label: 'Departments', value: 5, available: true, reason: null },
            {
              key: 'audit.events.24h',
              label: 'Audit events (last 24 h)',
              value: 12,
              available: true,
              reason: null,
            },
          ],
          widgets: [],
        }),
      ),
    );
    fixture.destroy();
    service.getSummary.calls.reset();
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const chart = element.querySelector('app-metrics-chart canvas');
    expect(chart).not.toBeNull();
    expect(element.querySelector('[data-testid="chart-note"]')).toBeNull();
  });

  it('renders an error state with problem details and retries the request', async () => {
    service.getSummary.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 503,
            statusText: 'Service Unavailable',
            error: {
              type: 'about:blank',
              title: 'Service unavailable',
              status: 503,
              detail: 'Dashboard unavailable for maintenance.',
              correlationId: 'corr-42',
            },
          }),
      ),
    );

    fixture.destroy();
    service.getSummary.calls.reset();
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const errorState = element.querySelector('[data-testid="dashboard-error"]');
    expect(errorState).not.toBeNull();
    expect(errorState?.textContent).toContain('Dashboard unavailable for maintenance.');
    expect(errorState?.textContent).toContain('corr-42');
    expect(element.querySelector('app-metrics-chart')).toBeNull();

    service.getSummary.and.returnValue(of(makeSummary()));
    const retryButton = element.querySelector(
      '[data-testid="dashboard-retry"]',
    ) as HTMLButtonElement | null;
    expect(retryButton).not.toBeNull();
    retryButton?.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(service.getSummary).toHaveBeenCalledTimes(2);
    expect(element.querySelector('[data-testid="dashboard-error"]')).toBeNull();
    expect(element.querySelectorAll('[data-testid="metric-card"]').length).toBe(2);
  });
});
