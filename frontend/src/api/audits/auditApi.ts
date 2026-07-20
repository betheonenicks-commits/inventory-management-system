import { httpClient } from '../httpClient'
import type {
  Audit,
  AuditAssignment,
  AuditCertificate,
  AuditCycleTrend,
  AuditExceptionReport,
  AuditFinding,
  AuditFindingReconciliation,
  FindingEvidence,
  AuditProgress,
  AuditStatus,
  AuditSummary,
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
  // US-DSH-05: optional planned date, plotted by the dashboard's audit calendar.
  scheduledDate?: string
  // US-AUD-20: optional statistical sampling; omitted = full 100% audit.
  samplingConfidenceLevel?: number
  samplingMarginOfError?: number
}

// US-AUD-20: sample-size preview for a prospective scope, before the audit is created.
export interface SampleSizePreview {
  populationSize: number
  confidenceLevel: number
  marginOfError: number
  sampleSize: number
}

export function fetchSampleSizePreview(input: {
  scopeOrgNodeId?: string
  scopeCategoryId?: string
  assetIds?: string[]
  confidenceLevel: number
  marginOfError?: number
}) {
  return httpClient.post<SampleSizePreview>('/audits/sample-size', input).then((r) => r.data)
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

/** No audits:read required - see AuditController.pickable(). */
export function fetchPickableAudits() {
  return httpClient.get<AuditSummary[]>('/audits/pickable').then((r) => r.data)
}

export function fetchAudit(id: string) {
  return httpClient.get<Audit>(`/audits/${id}`).then((r) => r.data)
}

export function createAudit(payload: AuditCreatePayload) {
  return httpClient.post<Audit>('/audits', payload).then((r) => r.data)
}

// US-AUD-18: cross-cycle audit analytics (missing-rate + completion-time trends).
export function fetchCrossCycleTrends() {
  return httpClient.get<AuditCycleTrend[]>('/audits/analytics/cross-cycle').then((r) => r.data)
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

/** US-AUD-15: the certificate as a formal, downloadable PDF document. */
export async function downloadAuditCertificatePdf(id: string, auditName: string) {
  const response = await httpClient.get(`/audits/${id}/certificate`, {
    params: { format: 'pdf' },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(response.data as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `audit-certificate-${auditName}.pdf`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** US-AUD-11: photo evidence, brokered through the backend (US-PLAT-02) - never a direct object-store URL. */
export function fetchFindingEvidence(auditId: string, findingId: string) {
  return httpClient
    .get<FindingEvidence[]>(`/audits/${auditId}/findings/${findingId}/evidence`)
    .then((r) => r.data)
}

export function uploadFindingEvidence(auditId: string, findingId: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return httpClient
    .post<FindingEvidence>(`/audits/${auditId}/findings/${findingId}/evidence`, form)
    .then((r) => r.data)
}

/** Fetches the image bytes with auth and hands back an object URL the caller must revoke. */
export function fetchFindingEvidenceBlobUrl(auditId: string, findingId: string, attachmentId: string) {
  return httpClient
    .get<Blob>(`/audits/${auditId}/findings/${findingId}/evidence/${attachmentId}`, { responseType: 'blob' })
    .then((r) => URL.createObjectURL(r.data))
}
