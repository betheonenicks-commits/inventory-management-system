import { httpClient } from '../httpClient'

// FR-ORG-03: departments/cost centers. list/get are open to any authenticated
// user (same as persons), so an Inventory Manager can pick one as a custodian.
export interface Department {
  id: string
  version: number
  name: string
  costCenterCode: string
  active: boolean
}

export function fetchDepartments() {
  return httpClient.get<Department[]>('/departments').then((r) => r.data)
}

export interface DepartmentCreatePayload {
  name: string
  costCenterCode: string
}

// US-ORG-03: department CRUD - previously fully built with no admin UI to reach it.
export function createDepartment(payload: DepartmentCreatePayload) {
  return httpClient.post<Department>('/departments', payload).then((r) => r.data)
}

export interface DepartmentUpdatePayload {
  name?: string
  costCenterCode?: string
  active?: boolean
  version: number
}

export function updateDepartment(id: string, payload: DepartmentUpdatePayload) {
  return httpClient.patch<Department>(`/departments/${id}`, payload).then((r) => r.data)
}

// Blocked with 409 (dependent assets/people) rather than a raw FK error.
export function deleteDepartment(id: string) {
  return httpClient.delete<void>(`/departments/${id}`).then((r) => r.data)
}
