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
