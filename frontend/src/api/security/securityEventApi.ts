import { httpClient } from '../httpClient'
import type { PageResponse } from '../../features/assets/types'

// Mirrors backend/src/main/java/com/iams/sec/domain/SecurityEventType.java
export type SecurityEventType =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'PERMISSION_DENIED'
  | 'ROLE_ASSIGNED'
  | 'USER_DEACTIVATED'
  | 'ACCOUNT_LOCKED'
  | 'ACCOUNT_UNLOCKED'
  | 'LOGOUT'
  | 'LOGOUT_ALL'
  | 'REFRESH_TOKEN_REUSE_DETECTED'
  | 'SESSION_EXPIRED'
  | 'RETENTION_PURGE_EXECUTED'
  | 'PERSON_ANONYMIZED'
  | 'SERVICE_ACCOUNT_CREATED'
  | 'SERVICE_ACCOUNT_REVOKED'
  | 'INTEGRATION_CREATED'
  | 'INTEGRATION_ENABLED'
  | 'INTEGRATION_DISABLED'
  | 'INTEGRATION_DELETED'
  | 'REPORT_EXPORTED'
  | 'AUDIT_SUBMITTED'

// The one shared list of all event types, for any filter dropdown that needs them.
export const SECURITY_EVENT_TYPES: SecurityEventType[] = [
  'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'PERMISSION_DENIED', 'ROLE_ASSIGNED', 'USER_DEACTIVATED',
  'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED', 'LOGOUT', 'LOGOUT_ALL', 'REFRESH_TOKEN_REUSE_DETECTED',
  'SESSION_EXPIRED', 'RETENTION_PURGE_EXECUTED', 'PERSON_ANONYMIZED', 'SERVICE_ACCOUNT_CREATED',
  'SERVICE_ACCOUNT_REVOKED', 'INTEGRATION_CREATED', 'INTEGRATION_ENABLED', 'INTEGRATION_DISABLED',
  'INTEGRATION_DELETED', 'REPORT_EXPORTED', 'AUDIT_SUBMITTED',
]

export interface SecurityEvent {
  id: string
  eventType: SecurityEventType
  actorUserId: string | null
  usernameAttempted: string | null
  ipAddress: string | null
  detail: string | null
  createdAt: string
}

export interface SecurityEventSearchParams {
  userId?: string
  eventType?: SecurityEventType
  from?: string
  to?: string
  page?: number
  size?: number
}

/** US-SEC-11: search/filter the Security & Access Log - previously reachable by no frontend page at all. */
export function searchSecurityEvents(params: SecurityEventSearchParams) {
  return httpClient.get<PageResponse<SecurityEvent>>('/security-events', { params }).then((r) => r.data)
}
