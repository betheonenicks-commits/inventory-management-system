import { httpClient } from '../httpClient'
import type {
  Audit,
  AuditAssignment,
  AuditCertificate,
  AuditExceptionReport,
  AuditFinding,
  AuditFindingReconciliation,
  AuditProgress,
  AuditStatus,
  AuditType,
  AssetCondition,
} from '../../features/audits/types'

export interface AuditCreatePayload {
  name: string
  auditType: AuditType
  scopeOrgNodeId?: string
  scopeCategoryId?: string
  assetIds?: string[]
  nominalApproverId: string
}

export interface AuditScanPayload {
  assetId: string
  condition: AssetCondition
  remarks?: string
  deviceId?: string
}

export function fetchAudits(status?: AuditStatus) {
  return httpClient.get<Audit[]>('/audits', { params: status ? { status } : undefined }).then((r) => r.data)
}

export function fetchAudit(id: string) {
  return httpClient.get<Audit>(`/audits/${id}`).then((r) => r.data)
}

export function createAudit(payload: AuditCreatePayload) {
  return httpClient.post<Audit>('/audits', payload).then((r) => r.data)
}

export function fetchAuditProgress(id: string) {
  return httpClient.get<AuditProgress>(`/audits/${id}/progress`).then((r) => r.data)
}

export function fetchAuditAssignments(id: string) {
  return httpClient.get<AuditAssignment[]>(`/audits/${id}/assignments`).then((r) => r.data)
}

export function assignAuditor(id: string, auditorUserId: string, subScope?: string) {
  return httpClient
    .post<AuditAssignment>(`/audits/${id}/assignments`, { auditorUserId, subScope })
    .then((r) => r.data)
}

export function unassignAuditor(id: string, assignmentId: string) {
  return httpClient.delete<AuditAssignment>(`/audits/${id}/assignments/${assignmentId}`).then((r) => r.data)
}

export function recordScan(id: string, payload: AuditScanPayload) {
  return httpClient.post<AuditFinding>(`/audits/${id}/scans`, payload).then((r) => r.data)
}

export function submitAudit(id: string, password: string, signatureName?: string) {
  return httpClient.post<Audit>(`/audits/${id}/submit`, { password, signatureName }).then((r) => r.data)
}

export function approveAudit(id: string) {
  return httpClient.post<Audit>(`/audits/${id}/approve`).then((r) => r.data)
}

export function rejectAudit(id: string, reason: string) {
  return httpClient.post<Audit>(`/audits/${id}/reject`, { reason }).then((r) => r.data)
}

/** US-AUD-14: no-ops with a 409 until the configured pending-approval threshold has actually passed. */
export function escalateAudit(id: string) {
  return httpClient.post<Audit>(`/audits/${id}/escalate`).then((r) => r.data)
}

/** US-AUD-21: reconcile a Missing finding found later, outside any active audit - a new linked record, never an edit. */
export function reconcileFinding(auditId: string, findingId: string, foundLocationNote: string) {
  return httpClient
    .post<AuditFindingReconciliation>(`/audits/${auditId}/findings/${findingId}/reconcile`, { foundLocationNote })
    .then((r) => r.data)
}

export function fetchAuditExceptions(id: string) {
  return httpClient.get<AuditExceptionReport>(`/audits/${id}/exceptions`).then((r) => r.data)
}

export function fetchAuditCertificate(id: string) {
  return httpClient.get<AuditCertificate>(`/audits/${id}/certificate`).then((r) => r.data)
}
