import { httpClient } from '../httpClient'

export interface SystemHealth {
  status: string
  components: Record<string, string>
  checkedAt: string
}

// US-USR-05 (AC-USR-05-H): the System Operator's technical view of app health,
// gated system:read (a role holding only system:read/write, no business-data
// permission, can reach this and nothing else).
export function fetchSystemHealth() {
  return httpClient.get<SystemHealth>('/system/health').then((r) => r.data)
}
