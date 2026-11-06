import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { RUNTIME_CONFIG } from '../config/runtime-config';
import { AuthService } from './auth.service';


export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const config = inject(RUNTIME_CONFIG);

  const api = new URL(config.apiBaseUrl, globalThis.location.origin);
  const target = new URL(request.url, globalThis.location.origin);
  const basePath = api.pathname.replace(/\/+$/, '');
  if (target.origin !== api.origin ||
      !(target.pathname === basePath || target.pathname.startsWith(`${basePath}/`))) {
    return next(request);
  }

  return from(auth.getAccessToken()).pipe(
    catchError(() => {
      // No usable token in memory: the session cannot continue.
      void auth.logout();
      return throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            url: request.url,
            error: { title: 'Not authenticated', status: 401, detail: 'No active session.' },
          }),
      );
    }),
    switchMap((accessToken) =>
      next(withBearer(request, accessToken)).pipe(
        catchError((error: unknown) => {
          if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
            return throwError(() => error);
          }

          return from(auth.refreshAccessToken()).pipe(
            catchError(() => {
              // The refresh failed: end the session and keep the original 401.
              void auth.logout();
              return throwError(() => error);
            }),
            switchMap((refreshedToken) =>
              next(withBearer(request, refreshedToken)).pipe(
                catchError((retryError: unknown) => {
                  if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
                    void auth.logout();
                  }
                  return throwError(() => retryError);
                }),
              ),
            ),
          );
        }),
      ),
    ),
  );
};

function withBearer(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
