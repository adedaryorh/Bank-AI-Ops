import { InjectionToken } from '@angular/core';


export interface RuntimeConfig {
  readonly apiBaseUrl: string;
  readonly oidcAuthority: string;
  readonly oidcClientId: string;
  readonly oidcRedirectUri: string;
  readonly oidcScopes: string;
  readonly postLogoutRedirectUri: string;
  readonly appName: string;
}

declare global {
  interface Window {
    __BANKOPS_CONFIG__?: Partial<RuntimeConfig>;
  }
}

export const DEFAULT_RUNTIME_CONFIG: RuntimeConfig = {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  oidcAuthority: 'http://localhost:8180/realms/bankops',
  oidcClientId: 'bankops-frontend',
  oidcRedirectUri: 'http://localhost:4200/auth/callback',
  oidcScopes: 'openid profile email',
  postLogoutRedirectUri: 'http://localhost:4200/',
  appName: 'BankOps AI',
};

/**
 * Reads the optional global configuration and merges it over the defaults.
 * Empty or non-string values are ignored so a partial deployment config cannot
 * break the app with an empty authority URL.
 */
export function loadRuntimeConfig(source: Window | undefined = globalThis.window): RuntimeConfig {
  const overrides = source?.__BANKOPS_CONFIG__;
  const result: { -readonly [K in keyof RuntimeConfig]: string } = { ...DEFAULT_RUNTIME_CONFIG };

  if (!overrides || typeof overrides !== 'object') {
    return result;
  }

  for (const key of Object.keys(DEFAULT_RUNTIME_CONFIG) as (keyof RuntimeConfig)[]) {
    const value = overrides[key];
    if (typeof value === 'string' && value.trim().length > 0) {
      result[key] = value.trim();
    }
  }

  return {
    ...result,
    apiBaseUrl: result.apiBaseUrl.replace(/\/+$/, ''),
    oidcAuthority: result.oidcAuthority.replace(/\/+$/, ''),
  };
}

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('BANKOPS_RUNTIME_CONFIG', {
  providedIn: 'root',
  factory: () => loadRuntimeConfig(),
});
