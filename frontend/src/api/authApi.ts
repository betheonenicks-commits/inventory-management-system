import { httpClient } from './httpClient'

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export function login(username: string, password: string) {
  return httpClient.post<LoginResponse>('/auth/login', { username, password }).then((r) => r.data)
}

// US-SEC-01: ends exactly this session server-side (the refresh token is
// revoked, not just forgotten client-side) - best-effort from the caller's
// perspective, since the user is signed out locally regardless of whether
// this call succeeds.
export function logout(refreshToken: string) {
  return httpClient.post('/auth/logout', { refreshToken }).then(() => undefined)
}

export interface MeResponse {
  id: string
  username: string
  displayName: string
  roleCodes: string[]
  orgScopeNodeId: string | null
  permissions: string[]
}

// The stored AuthUser (Zustand) never carries the user's own id - this is
// for the rare case something needs "my own id" (US-LIF-15's delegation
// picker needs its own delegatorUserId), not routine UI permission checks.
export function fetchMe() {
  return httpClient.get<MeResponse>('/auth/me').then((r) => r.data)
}

// US-SEC-09: self-service unlock. Both calls return the same shape (or a
// generic 400 on a bad/expired code) regardless of whether the username
// exists or is actually locked - the backend never leaks that either way.
export function requestUnlock(username: string) {
  return httpClient.post('/auth/unlock/request', { username }).then(() => undefined)
}

export function confirmUnlock(token: string) {
  return httpClient.post('/auth/unlock/confirm', { token }).then(() => undefined)
}

// US-SEC-06: step-up re-authentication. A wrong password here is a distinct,
// already-authenticated re-confirmation - not a login attempt - so it doesn't
// count toward account lockout.
export function stepUp(password: string) {
  return httpClient.post('/auth/step-up', { password }).then(() => undefined)
}
