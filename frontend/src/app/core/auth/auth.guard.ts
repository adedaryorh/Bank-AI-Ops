import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  try {
    await auth.startLogin(state.url);
  } catch {
    return router.createUrlTree(['/auth/callback'], {
      queryParams: { error: 'login_unavailable' },
    });
  }
  return false;
};

export const adminGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    try {
      await auth.startLogin(state.url);
    } catch {
      return router.createUrlTree(['/auth/callback'], {
        queryParams: { error: 'login_unavailable' },
      });
    }
    return false;
  }

  return auth.hasRole('ADMIN') ? true : router.createUrlTree(['/forbidden']);
};

export function rolesGuard(...roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return roles.some((role) => auth.hasRole(role)) ? true : router.createUrlTree(['/forbidden']);
  };
}
