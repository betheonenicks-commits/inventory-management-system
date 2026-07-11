import { httpClient } from './httpClient'

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export function login(username: string, password: string) {
  return httpClient.post<LoginResponse>('/auth/login', { username, password }).then((r) => r.data)
}
