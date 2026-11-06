import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { DEFAULT_RUNTIME_CONFIG, RUNTIME_CONFIG } from '../config/runtime-config';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const API = DEFAULT_RUNTIME_CONFIG.apiBaseUrl;
const SUMMARY_URL = `${API}/dashboard/summary`;


function settle(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', [
      'getAccessToken',
      'refreshAccessToken',
      'logout',
      'isAuthenticated',
      'hasRole',
    ]);
    auth.getAccessToken.and.resolveTo('access-token-1');
    auth.refreshAccessToken.and.resolveTo('access-token-2');
    auth.logout.and.resolveTo();
    auth.isAuthenticated.and.returnValue(true);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: RUNTIME_CONFIG, useValue: DEFAULT_RUNTIME_CONFIG },
        { provide: AuthService, useValue: auth },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds the bearer token to API requests', async () => {
    const responsePromise = firstValueFrom(http.get(SUMMARY_URL));
    await settle();

    const request = httpMock.expectOne(SUMMARY_URL);
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token-1');
    request.flush({ ok: true });

    expect(await responsePromise).toEqual({ ok: true });
    expect(auth.getAccessToken).toHaveBeenCalled();
  });

  it('does not attach a bearer token to non-API requests', async () => {
    const responsePromise = firstValueFrom(http.get('/i18n/en.json'));
    await settle();

    const request = httpMock.expectOne('/i18n/en.json');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});

    await responsePromise;
    expect(auth.getAccessToken).not.toHaveBeenCalled();
  });

  for (const url of [
    `${API}.evil.example/collect`,
    `${API}-other/collect`,
    `${API}/../outside`,
    API.replace('localhost', 'localhost.evil.example'),
  ]) {
    it(`does not leak tokens to ${url}`, async () => {
      const response = firstValueFrom(http.get(url));
      const request = httpMock.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBeFalse();
      request.flush({});
      await response;
      expect(auth.getAccessToken).not.toHaveBeenCalled();
    });
  }

  it('refreshes once on 401 and retries with the new token', async () => {
    const responsePromise = firstValueFrom(http.get(SUMMARY_URL));
    await settle();

    const firstRequest = httpMock.expectOne(SUMMARY_URL);
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer access-token-1');
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    await settle();

    const retryRequest = httpMock.expectOne(SUMMARY_URL);
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer access-token-2');
    retryRequest.flush({ ok: true });

    expect(await responsePromise).toEqual({ ok: true });
    expect(auth.refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('logs out and fails when the refresh on 401 fails', async () => {
    auth.refreshAccessToken.and.rejectWith(new Error('refresh failed'));

    const responsePromise = firstValueFrom(http.get(SUMMARY_URL)).catch((error: unknown) => error);
    await settle();

    const firstRequest = httpMock.expectOne(SUMMARY_URL);
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    await settle();

    const error: unknown = await responsePromise;

    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect((error as HttpErrorResponse).status).toBe(401);
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(auth.refreshAccessToken).toHaveBeenCalledTimes(1);
  });
});
