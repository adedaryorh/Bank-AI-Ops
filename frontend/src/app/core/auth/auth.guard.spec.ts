import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthService } from './auth.service';
import { adminGuard, authGuard } from './auth.guard';

describe('auth guards', () => {
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', [
      'isAuthenticated',
      'startLogin',
      'hasRole',
    ]);
    auth.startLogin.and.resolveTo();

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  function runGuard(
    guard: CanActivateFn,
    url = '/dashboard',
  ): Promise<boolean | UrlTree> | boolean | UrlTree {
    const route = {} as ActivatedRouteSnapshot;
    const state = { url } as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => guard(route, state)) as
      | Promise<boolean | UrlTree>
      | boolean
      | UrlTree;
  }

  it('starts the login and blocks navigation when unauthenticated', async () => {
    auth.isAuthenticated.and.returnValue(false);

    const result = await runGuard(authGuard, '/admin/users');

    expect(auth.startLogin).toHaveBeenCalledOnceWith('/admin/users');
    expect(result).toBeFalse();
  });

  it('allows navigation when authenticated', async () => {
    auth.isAuthenticated.and.returnValue(true);

    const result = await runGuard(authGuard, '/dashboard');

    expect(result).toBeTrue();
    expect(auth.startLogin).not.toHaveBeenCalled();
  });

  it('sends authenticated non-admins to the forbidden page', async () => {
    auth.isAuthenticated.and.returnValue(true);
    auth.hasRole.and.returnValue(false);

    const result = await runGuard(adminGuard, '/admin/users');

    expect(result).toEqual(jasmine.any(UrlTree));
    expect((result as UrlTree).toString()).toBe('/forbidden');
    expect(auth.hasRole).toHaveBeenCalledWith('ADMIN');
  });

  it('allows administrators through the admin guard', async () => {
    auth.isAuthenticated.and.returnValue(true);
    auth.hasRole.and.returnValue(true);

    const result = await runGuard(adminGuard, '/admin/users');

    expect(result).toBeTrue();
  });
});
