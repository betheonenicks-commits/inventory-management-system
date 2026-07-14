// Mirrors backend/src/main/java/com/iams/compliance/api/dto/*.java and com.iams.compliance.domain enums.

export type RetentionEntityType = 'SECURITY_EVENT_LOG' | 'DISPOSED_ASSET' | 'PERSON' | 'ASSET_HISTORY_EVENT' | 'AUDIT_RECORD'
export type RetentionExpiryAction = 'DELETE' | 'ANONYMIZE' | 'HOLD_ELIGIBLE'
export type LegalHoldScopeType = 'ASSET' | 'AUDIT'
export type AccessibilityAuditOutcome = 'PASS' | 'PASS_WITH_EXCEPTIONS' | 'FAIL'

export interface RetentionPolicy {
  id: string
  version: number
  entityType: RetentionEntityType
  retentionPeriodDays: number
  expiryAction: RetentionExpiryAction
}

export interface LegalHold {
  id: string
  version: number
  scopeType: LegalHoldScopeType
  scopeId: string
  reason: string
  active: boolean
  liftedBy: string | null
  liftedAt: string | null
  liftReason: string | null
}

export interface PersonAnonymization {
  id: string
  fullName: string
  active: boolean
  anonymizedAt: string | null
}

export interface PrivacyNoticeConfig {
  id: string
  version: number
  fieldName: string
  noticeText: string
}

export interface AccessibilityAuditRecord {
  id: string | null
  version: number
  auditDate: string | null
  outcome: AccessibilityAuditOutcome | null
  notes: string | null
}

export interface OutboundIntegrationFlag {
  id: string
  version: number
  name: string
  enabled: boolean
  complianceReviewNote: string | null
}

export interface DataResidencyView {
  allStoresOnPremises: boolean
  enabledOutboundFlows: OutboundIntegrationFlag[]
}
