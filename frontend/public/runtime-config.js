/*
 * Runtime configuration for BankOps AI.
 *
 * Deployments may replace this file (or inject an equivalent script before the
 * Angular bundle) to point the SPA at their API and identity provider.
 * This file must never contain secrets: the OIDC client is public and uses
 * authorization code + PKCE.
 */
window.__BANKOPS_CONFIG__ = window.__BANKOPS_CONFIG__ || {
  apiBaseUrl: 'http://localhost:8080/api/v1',
  oidcAuthority: 'http://localhost:8180/realms/bankops',
  oidcClientId: 'bankops-frontend',
  oidcRedirectUri: 'http://localhost:4200/auth/callback',
  oidcScopes: 'openid profile email',
  postLogoutRedirectUri: 'http://localhost:4200/',
  appName: 'BankOps AI',
};
