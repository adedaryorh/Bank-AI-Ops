import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  provideTransloco,
  TranslocoLoader,
  TranslocoService,
  Translation,
} from '@jsverse/transloco';
import { Observable, of } from 'rxjs';
import { signal } from '@angular/core';
import { AppShell } from './app-shell';
import { AuthService } from '../auth/auth.service';
import { CurrentUser } from '../api/api.models';

const TRANSLATIONS = {
  app: {
    name: 'BankOps AI',
    tagline: 'Decision support for bank employees',
    footerNotice:
      'Synthetic data only. Not financial advice. Decision support for bank employees; the system never approves or rejects anything.',
  },
  nav: {
    primary: 'Primary navigation',
    dashboard: 'Dashboard',
    adminUsers: 'User administration',
    collapse: 'Collapse sidebar',
    expand: 'Expand sidebar',
    openMenu: 'Open navigation',
    closeMenu: 'Close navigation',
  },
  topbar: {
    language: 'Language',
    english: 'English',
    german: 'German',
    theme: 'Toggle theme',
    lightTheme: 'Light theme',
    darkTheme: 'Dark theme',
    account: 'Account menu',
    signedInAs: 'Signed in as',
    department: 'Department',
    roles: 'Roles',
    logout: 'Sign out',
  },
  work: {
    cases: 'Cases',
    documents: 'Documents',
    policies: 'Policy evidence',
    activity: 'Audit & AI activity',
    aml: 'AML investigations',
    reviewQueue: 'Awaiting human review',
  },
  roles: {
    OPERATIONS_OFFICER: 'Operations officer',
  },
};

class ShellTestLoader implements TranslocoLoader {
  getTranslation(): Observable<Translation> {
    return of(TRANSLATIONS);
  }
}

describe('AppShell', () => {
  let fixture: ComponentFixture<AppShell>;
  let warn: jasmine.Spy;
  let admin: ReturnType<typeof signal<boolean>>;

  const user: CurrentUser = {
    id: '11111111-1111-1111-1111-111111111111',
    subject: 'ops@bankops.local',
    email: 'ops@bankops.local',
    displayName: 'Otto Operations',
    department: 'OPS',
    departmentName: 'Banking Operations',
    roles: ['OPERATIONS_OFFICER'],
  };

  beforeEach(async () => {
    warn = spyOn(console, 'warn').and.callThrough();
    admin = signal(false);
    const logout = jasmine.createSpy('logout').and.resolveTo();

    const authStub = {
      user: signal(user).asReadonly(),
      isAdmin: () => admin(),
      hasRole: (role: string) => role === 'ADMIN' && admin(),
      logout,
    } as unknown as AuthService;

    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        ...provideTransloco({
          config: { availableLangs: ['en', 'de'], defaultLang: 'en' },
          loader: ShellTestLoader,
        }),
      ],
    }).compileComponents();

    const transloco = TestBed.inject(TranslocoService);
    transloco.setTranslation(TRANSLATIONS, 'en');
    transloco.setActiveLang('en');

    fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => {
    const missing = warn.calls.allArgs().filter(args => args.some(value => String(value).includes('Missing translation')));
    expect(missing).withContext('Shell translation keys must exist in the test fixture').toEqual([]);
  });

  it('renders the shell, footer notice and only the routes allowed for the role', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('BankOps AI');
    expect(element.textContent).toContain('Otto Operations');
    expect(element.textContent).toContain('Banking Operations');
    expect(element.textContent).toContain(
      'Synthetic data only. Not financial advice. Decision support for bank employees; the system never approves or rejects anything.',
    );
    expect(element.querySelector('a[routerLink="/"]')).not.toBeNull();
    expect(element.querySelector('a[routerLink="/admin/users"]')).toBeNull();
  });

  it('shows the admin route only for administrators', () => {
    admin.set(true);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('a[routerLink="/admin/users"]')).not.toBeNull();
  });
});
