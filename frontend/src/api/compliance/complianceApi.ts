import { httpClient } from '../httpClient'
import type {
  AccessibilityAuditOutcome,
  AccessibilityAuditRecord,
  DataResidencyView,
  LegalHold,
  LegalHoldScopeType,
  OutboundIntegrationFlag,
  PersonAnonymization,
  PrivacyNoticeConfig,
  RetentionEntityType,
  RetentionExpiryAction,
  RetentionPolicy,
} from '../../features/compliance/types'

// --- Retention policy (US-CMP-01) ---

export function fetchRetentionPolicies() {
  return httpClient.get<RetentionPolicy[]>('/compliance/retention-policies').then((r) => r.data)
}

export function saveRetentionPolicy(entityType: RetentionEntityType, retentionPeriodDays: number, expiryAction: RetentionExpiryAction) {
  return httpClient
    .put<RetentionPolicy>('/compliance/retention-policies', { entityType, retentionPeriodDays, expiryAction })
    .then((r) => r.data)
}

export function purgeSecurityEventLog() {
  return httpClient.post<{ deletedCount: number }>('/compliance/retention-policies/security-event-log/purge').then((r) => r.data)
}

// --- Legal holds (US-CMP-06) ---

export function fetchLegalHolds(scopeType?: LegalHoldScopeType) {
  return httpClient.get<LegalHold[]>('/compliance/legal-holds', { params: scopeType ? { scopeType } : undefined }).then((r) => r.data)
}

export function placeLegalHold(scopeType: LegalHoldScopeType, scopeId: string, reason: string) {
  return httpClient.post<LegalHold>('/compliance/legal-holds', { scopeType, scopeId, reason }).then((r) => r.data)
}

export function liftLegalHold(id: string, liftReason: string) {
  return httpClient.post<LegalHold>(`/compliance/legal-holds/${id}/lift`, { liftReason }).then((r) => r.data)
}

// --- Person anonymization (US-CMP-02 / US-LIF-14) ---

export function fetchAnonymizationEligiblePersons() {
  return httpClient.get<PersonAnonymization[]>('/compliance/person-anonymization/eligible').then((r) => r.data)
}

export function anonymizePerson(personId: string) {
  return httpClient.post<PersonAnonymization>(`/compliance/person-anonymization/${personId}/anonymize`).then((r) => r.data)
}

// --- Privacy notices (US-CMP-03) ---

export function fetchPrivacyNotices() {
  return httpClient.get<PrivacyNoticeConfig[]>('/compliance/privacy-notices').then((r) => r.data)
}

export function savePrivacyNotice(fieldName: string, noticeText: string) {
  return httpClient.put<PrivacyNoticeConfig>('/compliance/privacy-notices', { fieldName, noticeText }).then((r) => r.data)
}

export function deletePrivacyNotice(id: string) {
  return httpClient.delete(`/compliance/privacy-notices/${id}`)
}

// --- Accessibility audit (US-CMP-04) ---

export function fetchAccessibilityAudit() {
  return httpClient.get<AccessibilityAuditRecord>('/compliance/accessibility-audit').then((r) => r.data)
}

export function recordAccessibilityAudit(auditDate: string, outcome: AccessibilityAuditOutcome, notes?: string) {
  return httpClient.put<AccessibilityAuditRecord>('/compliance/accessibility-audit', { auditDate, outcome, notes }).then((r) => r.data)
}

// --- Data residency (US-CMP-05) ---

export function fetchDataResidency() {
  return httpClient.get<DataResidencyView>('/compliance/data-residency').then((r) => r.data)
}

export function fetchOutboundFlows() {
  return httpClient.get<OutboundIntegrationFlag[]>('/compliance/data-residency/outbound-flows').then((r) => r.data)
}

export function saveOutboundFlow(name: string, enabled: boolean, complianceReviewNote?: string) {
  return httpClient
    .put<OutboundIntegrationFlag>('/compliance/data-residency/outbound-flows', { name, enabled, complianceReviewNote })
    .then((r) => r.data)
}

export function deleteOutboundFlow(id: string) {
  return httpClient.delete(`/compliance/data-residency/outbound-flows/${id}`)
}
