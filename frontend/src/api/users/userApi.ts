import { httpClient } from '../httpClient'
import type { User, UserSummary } from '../../features/users/types'

export interface UserCreatePayload {
  username: string
  password: string
  displayName: string
  email?: string
  personId?: string
  orgScopeNodeId?: string
  roleCodes: string[]
}

export function fetchUsers() {
  return httpClient.get<User[]>('/users').then((r) => r.data)
}

/** Open to any authenticated user (no users:read needed) - see UserController.pickable(). */
export function fetchPickableUsers() {
  return httpClient.get<UserSummary[]>('/users/pickable').then((r) => r.data)
}

export function fetchUser(id: string) {
  return httpClient.get<User>(`/users/${id}`).then((r) => r.data)
}

export function createUser(payload: UserCreatePayload) {
  return httpClient.post<User>('/users', payload).then((r) => r.data)
}

export function deactivateUser(id: string, version: number) {
  return httpClient.post<User>(`/users/${id}/deactivate`, { version }).then((r) => r.data)
}
