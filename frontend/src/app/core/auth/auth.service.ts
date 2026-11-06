import { computed, inject, Injectable, signal } from '@angular/core';
import { CurrentUser } from '../api/api.models';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config/runtime-config';
import {
  clearPkceRecord,
  createOidcState,
  createPkcePair,
  PkceRecord,
  readPkceRecord,
  safeReturnUrl,
  savePkceRecord,
} from './pkce';

/** OIDC discovery document subset that this client needs. */
export interface OidcDiscovery {
  readonly issuer: string;
  readonly authorization_endpoint: string;
  readonly token_endpoint: string;
  readonly end_session_endpoint?: string;
  readonly jwks_uri?: string;
}

/** Tokens are held only in this in-memory signal. They are never persisted. */
export interface TokenSet {
  readonly accessToken: string;
  readonly idToken: string | null;
  readonly refreshToken: string | null;
  /** Epoch milliseconds at which the access token expires. */
  readonly expiresAt: number;
}

export type AuthErrorCode =
  | 'discovery_failed'
  | 'state_missing'
  | 'state_mismatch'
  | 'token_exchange_failed'
  | 'userinfo_failed'
  | 'not_authenticated'
  | 'session_expired'
  | 'no_refresh_token'
  | 'session_ended';

export class AuthError extends Error {
  constructor(
    readonly code: AuthErrorCode,
    message: string,
  ) {
    super(message);
    this.name = 'AuthError';
  }
}

interface TokenEndpointResponse {
  readonly access_token?: string;
  readonly id_token?: string;
  readonly refresh_token?: string;
  readonly expires_in?: number;
  readonly token_type?: string;
  readonly error?: string;
  readonly error_description?: string;
}

const REFRESH_LEEWAY_MS = 30_000;
const DEFAULT_TOKEN_LIFETIME_SECONDS = 300;
const MAX_TIMEOUT_MS = 2_147_483_647;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly config: RuntimeConfig = inject(RUNTIME_CONFIG);

  private readonly _tokens = signal<TokenSet | null>(null);
  private readonly _user = signal<CurrentUser | null>(null);

  /** Read-only in-memory session signals. Tokens never leave the browser tab. */
  readonly tokens = this._tokens.asReadonly();
  readonly user = this._user.asReadonly();
  readonly expiresAt = computed(() => this._tokens()?.expiresAt ?? null);
  readonly isAdmin = computed(() => this.hasRole('ADMIN'));

  private discoveryPromise: Promise<OidcDiscovery> | null = null;
  private refreshInFlight: Promise<string> | null = null;
  private refreshTimer: ReturnType<typeof setTimeout> | null = null;
  private loginInProgress = false;
  private sessionEpoch = 0;

  isAuthenticated(): boolean {
    const tokens = this._tokens();
    return tokens !== null && Date.now() < tokens.expiresAt;
  }

  hasRole(role: string): boolean {
    return this._user()?.roles.includes(role) ?? false;
  }

  discover(): Promise<OidcDiscovery> {
    if (!this.discoveryPromise) {
      this.discoveryPromise = this.fetchDiscovery().catch((error: unknown) => {
        this.discoveryPromise = null;
        throw error;
      });
    }
    return this.discoveryPromise;
  }

  async startLogin(returnUrl: string): Promise<void> {
    if (this.loginInProgress) {
      return;
    }
    this.loginInProgress = true;
    try {
      const discovery = await this.discover();
      const pkce = await createPkcePair();
      const state = createOidcState();
      const record: PkceRecord = {
        verifier: pkce.verifier,
        state,
        returnUrl: safeReturnUrl(returnUrl),
        createdAt: Date.now(),
      };
      savePkceRecord(record);

      const authorizeUrl = new URL(discovery.authorization_endpoint);
      authorizeUrl.searchParams.set('response_type', 'code');
      authorizeUrl.searchParams.set('client_id', this.config.oidcClientId);
      authorizeUrl.searchParams.set('redirect_uri', this.config.oidcRedirectUri);
      authorizeUrl.searchParams.set('scope', this.config.oidcScopes);
      authorizeUrl.searchParams.set('state', state);
      authorizeUrl.searchParams.set('code_challenge', pkce.challenge);
      authorizeUrl.searchParams.set('code_challenge_method', pkce.method);
      window.location.assign(authorizeUrl.toString());
    } finally {
      this.loginInProgress = false;
    }
  }

  /** Exchanges the authorization code and loads the caller from the API. */
  async completeLogin(code: string, state: string): Promise<string> {
    const record = readPkceRecord();
    if (!record) {
      throw new AuthError('state_missing', 'The sign-in state is missing or expired. Start again.');
    }
    if (record.state !== state) {
      clearPkceRecord();
      throw new AuthError('state_mismatch', 'The sign-in state did not match. Start again.');
    }
    clearPkceRecord();

    const epoch = this.sessionEpoch;
    const discovery = await this.discover();
    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: this.config.oidcRedirectUri,
      client_id: this.config.oidcClientId,
      code_verifier: record.verifier,
    });

    const response = await this.postToken(discovery.token_endpoint, body, 'token_exchange_failed');
    if (epoch !== this.sessionEpoch) {
      throw new AuthError('session_ended', 'The sign-in was cancelled.');
    }
    this.storeTokens(response);

    try {
      await this.loadCurrentUser();
    } catch (error: unknown) {
      this.clearSession();
      throw error;
    }

    return safeReturnUrl(record.returnUrl);
  }

  async getAccessToken(): Promise<string> {
    const tokens = this._tokens();
    if (!tokens) {
      throw new AuthError('not_authenticated', 'No active session.');
    }
    if (tokens.expiresAt - Date.now() > REFRESH_LEEWAY_MS) {
      return tokens.accessToken;
    }
    if (!tokens.refreshToken) {
      throw new AuthError('session_expired', 'The session expired. Sign in again.');
    }
    return this.refreshAccessToken();
  }

  /** Single-flight refresh; concurrent callers share one token request. */
  refreshAccessToken(): Promise<string> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }
    const tokens = this._tokens();
    if (!tokens?.refreshToken) {
      return Promise.reject(new AuthError('no_refresh_token', 'No refresh token is available.'));
    }
    const refreshToken = tokens.refreshToken;

    this.refreshInFlight = (async () => {
      const epoch = this.sessionEpoch;
      const discovery = await this.discover();
      const body = new URLSearchParams({
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
        client_id: this.config.oidcClientId,
      });
      const response = await this.postToken(discovery.token_endpoint, body, 'token_exchange_failed');
      if (epoch !== this.sessionEpoch) {
        throw new AuthError('session_ended', 'The session ended during refresh.');
      }
      return this.storeTokens(response).accessToken;
    })().finally(() => {
      this.refreshInFlight = null;
    });

    return this.refreshInFlight;
  }

  async logout(): Promise<void> {
    const idToken = this._tokens()?.idToken ?? null;
    this.clearSession();

    try {
      const discovery = await this.discover();
      if (discovery.end_session_endpoint) {
        const url = new URL(discovery.end_session_endpoint);
        url.searchParams.set('client_id', this.config.oidcClientId);
        if (idToken) {
          url.searchParams.set('id_token_hint', idToken);
        }
        url.searchParams.set('post_logout_redirect_uri', this.config.postLogoutRedirectUri);
        window.location.assign(url.toString());
        return;
      }
    } catch {
      // Fall through to the local redirect.
    }
    window.location.assign('/');
  }


  private async fetchDiscovery(): Promise<OidcDiscovery> {
    const url = `${this.config.oidcAuthority}/.well-known/openid-configuration`;
    let response: Response;
    try {
      response = await fetch(url, {
        headers: { Accept: 'application/json' },
        credentials: 'omit',
        cache: 'no-store',
      });
    } catch {
      throw new AuthError('discovery_failed', 'The identity provider could not be reached.');
    }
    if (!response.ok) {
      throw new AuthError('discovery_failed', `Discovery failed with HTTP ${response.status}.`);
    }

    const document: unknown = await response.json();
    if (!isOidcDiscovery(document) || document.issuer !== this.config.oidcAuthority) {
      throw new AuthError('discovery_failed', 'The discovery document is incomplete.');
    }
    return document;
  }

  private async postToken(
    tokenEndpoint: string,
    body: URLSearchParams,
    errorCode: AuthErrorCode,
  ): Promise<TokenEndpointResponse> {
    let response: Response;
    try {
      response = await fetch(tokenEndpoint, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: body.toString(),
        credentials: 'omit',
        cache: 'no-store',
      });
    } catch {
      throw new AuthError(errorCode, 'The token endpoint could not be reached.');
    }

    const payload = (await response.json().catch(() => ({}))) as TokenEndpointResponse;
    if (!response.ok || payload.error) {
      const reason = payload.error_description ?? payload.error ?? `HTTP ${response.status}`;
      throw new AuthError(errorCode, reason);
    }
    if (!payload.access_token) {
      throw new AuthError(errorCode, 'The token response did not contain an access token.');
    }
    return payload;
  }

  private storeTokens(response: TokenEndpointResponse): TokenSet {
    if (!response.access_token) {
      throw new AuthError('token_exchange_failed', 'The token response did not contain an access token.');
    }
    const previous = this._tokens();
    const expiresIn =
      typeof response.expires_in === 'number' && response.expires_in > 0
        ? response.expires_in
        : DEFAULT_TOKEN_LIFETIME_SECONDS;

    const tokens: TokenSet = {
      accessToken: response.access_token,
      idToken: response.id_token ?? previous?.idToken ?? null,
      refreshToken: response.refresh_token ?? previous?.refreshToken ?? null,
      expiresAt: Date.now() + expiresIn * 1000,
    };

    this._tokens.set(tokens);
    this.scheduleProactiveRefresh(tokens);
    return tokens;
  }

  private scheduleProactiveRefresh(tokens: TokenSet): void {
    this.clearRefreshTimer();
    if (!tokens.refreshToken) {
      return;
    }
    const delay = tokens.expiresAt - Date.now() - REFRESH_LEEWAY_MS;
    this.refreshTimer = setTimeout(
      () => {
        this.refreshTimer = null;
        void this.refreshAccessToken().catch(() => {
          // The session cannot be renewed: end it and let Keycloak decide
          // whether the SSO session can be closed as well.
          void this.logout();
        });
      },
      Math.min(Math.max(delay, 0), MAX_TIMEOUT_MS),
    );
  }

  private clearRefreshTimer(): void {
    if (this.refreshTimer !== null) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  private async loadCurrentUser(): Promise<void> {
    const accessToken = this._tokens()?.accessToken;
    if (!accessToken) {
      throw new AuthError('not_authenticated', 'No access token is available.');
    }

    let response: Response;
    try {
      response = await fetch(`${this.config.apiBaseUrl}/auth/me`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${accessToken}` },
        credentials: 'omit',
        cache: 'no-store',
      });
    } catch {
      throw new AuthError('userinfo_failed', 'The user profile could not be loaded.');
    }

    if (!response.ok) {
      throw new AuthError('userinfo_failed', `The user profile could not be loaded (HTTP ${response.status}).`);
    }
    const user = (await response.json()) as CurrentUser;
    if (!user || typeof user.id !== 'string' || !Array.isArray(user.roles)) {
      throw new AuthError('userinfo_failed', 'The user profile response was invalid.');
    }
    this._user.set(user);
  }

  private clearSession(): void {
    this.sessionEpoch += 1;
    this.clearRefreshTimer();
    this._tokens.set(null);
    this._user.set(null);
    clearPkceRecord();
  }
}

function isOidcDiscovery(value: unknown): value is OidcDiscovery {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const document = value as Record<string, unknown>;
  return (
    typeof document['issuer'] === 'string' &&
    typeof document['authorization_endpoint'] === 'string' &&
    typeof document['token_endpoint'] === 'string' &&
    (document['end_session_endpoint'] === undefined || typeof document['end_session_endpoint'] === 'string')
  );
}
