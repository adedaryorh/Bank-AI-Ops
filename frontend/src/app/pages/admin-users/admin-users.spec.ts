import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  provideTransloco,
  TranslocoLoader,
  TranslocoService,
  Translation,
} from '@jsverse/transloco';
import { Observable, of, throwError } from 'rxjs';
import { AdminUser, PageResponse } from '../../core/api/api.models';
import { AdminUsersService } from '../../core/api/admin-users.service';
import { AdminUsersPage } from './admin-users';

const TRANSLATIONS = {
  admin: {
    title: 'User administration',
    subtitle: 'Provisioned users from the backend (ADMIN only).',
    loading: 'Loading users',
    total: '{{count}} users',
    name: 'Name',
    email: 'Email',
    department: 'Department',
    roles: 'Roles',
    createdAt: 'Created',
    lastSeenAt: 'Last seen',
    never: 'Never',
    emptyTitle: 'No users',
    emptyBody: 'The API returned an empty user directory.',
    errorTitle: 'Users could not be loaded',
    forbiddenTitle: 'Administrator role required',
    forbiddenBody: 'The API rejected this request with 403.',
    retry: 'Retry',
    back: 'Back to dashboard',
    pageLabel: 'Page {{page}} of {{pages}}',
    pageSize: 'Items per page',
  },
  errors: {
    forbidden: 'You do not have permission for this action.',
    unknown: 'An unexpected error occurred.',
    correlationId: 'Correlation ID',
    status: 'HTTP status',
  },
  roles: { ADMIN: 'Administrator', AUDITOR: 'Auditor' },
};

class AdminTestLoader implements TranslocoLoader {
  getTranslation(): Observable<Translation> {
    return of(TRANSLATIONS);
  }
}

const USERS: AdminUser[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    subject: 'admin@bankops.local',
    email: 'admin@bankops.local',
    displayName: 'Anna Admin',
    department: 'OPS',
    roles: ['ADMIN'],
    createdAt: '2026-10-24T12:00:00Z',
    lastSeenAt: '2026-10-24T14:00:00Z',
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    subject: 'auditor@bankops.local',
    email: 'auditor@bankops.local',
    displayName: 'Alina Auditor',
    department: 'AUDIT',
    roles: ['AUDITOR'],
    createdAt: '2026-10-24T12:00:00Z',
    lastSeenAt: null,
  },
];

function pageOf(content: AdminUser[]): PageResponse<AdminUser> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 };
}

describe('AdminUsersPage', () => {
  let fixture: ComponentFixture<AdminUsersPage>;
  let service: jasmine.SpyObj<AdminUsersService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<AdminUsersService>('AdminUsersService', ['list']);
    service.list.and.returnValue(of(pageOf(USERS)));

    await TestBed.configureTestingModule({
      imports: [AdminUsersPage],
      providers: [
        provideRouter([]),
        { provide: AdminUsersService, useValue: service },
        ...provideTransloco({
          config: { availableLangs: ['en', 'de'], defaultLang: 'en' },
          loader: AdminTestLoader,
        }),
      ],
    }).compileComponents();

    const transloco = TestBed.inject(TranslocoService);
    transloco.setTranslation(TRANSLATIONS, 'en');
    transloco.setActiveLang('en');

    fixture = TestBed.createComponent(AdminUsersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders the paginated Material table with users and roles', () => {
    const element: HTMLElement = fixture.nativeElement;
    const rows = element.querySelectorAll('tr.mat-mdc-row');

    expect(service.list).toHaveBeenCalledOnceWith(0, 20);
    expect(rows.length).toBe(2);
    expect(element.textContent).toContain('Anna Admin');
    expect(element.textContent).toContain('admin@bankops.local');
    expect(element.textContent).toContain('Administrator');
    expect(element.textContent).toContain('Never');
    expect(element.querySelector('mat-paginator')).not.toBeNull();
  });

  it('handles a 403 from the API gracefully without rendering data', async () => {
    service.list.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 403,
            statusText: 'Forbidden',
            error: {
              type: 'about:blank',
              title: 'Forbidden',
              status: 403,
              detail: 'ADMIN role required.',
              correlationId: 'corr-403',
            },
          }),
      ),
    );

    fixture.destroy();
    fixture = TestBed.createComponent(AdminUsersPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('[data-testid="admin-forbidden"]')).not.toBeNull();
    expect(element.textContent).toContain('Administrator role required');
    expect(element.textContent).toContain('ADMIN role required.');
    expect(element.querySelector('table')).toBeNull();
  });
});
