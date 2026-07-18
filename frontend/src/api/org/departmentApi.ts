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
