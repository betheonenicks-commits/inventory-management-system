import { httpClient } from '../httpClient'
import type { Role } from '../../features/roles/types'

export interface RoleCreatePayload {
  code: string
  name: string
  description?: string
  permissions: string[]
}

export interface RoleUpdatePayload {
  name?: string
  description?: string
  permissions?: string[]
  version: number
}

export function fetchRoles() {
  return httpClient.get<Role[]>('/roles').then((r) => r.data)
}

export function fetchRole(id: string) {
  return httpClient.get<Role>(`/roles/${id}`).then((r) => r.data)
}

export function createRole(payload: RoleCreatePayload) {
  return httpClient.post<Role>('/roles', payload).then((r) => r.data)
}

export function updateRole(id: string, payload: RoleUpdatePayload) {
  return httpClient.patch<Role>(`/roles/${id}`, payload).then((r) => r.data)
}

export function deleteRole(id: string) {
  return httpClient.delete(`/roles/${id}`)
}
