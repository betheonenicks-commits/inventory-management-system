// Mirrors backend/src/main/java/com/iams/audit/api/dto/*.java and com.iams.audit.domain enums.

export type AuditType = 'ANNUAL' | 'SPOT_CHECK' | 'BULK'
export type AuditStatus = 'IN_PROGRESS' | 'PENDING_APPROVAL' | 'CLOSED'
export type FindingStatus = 'VERIFIED' | 'MISSING' | 'OUT_OF_SCOPE' | 'SCOPE_CHANGED'
export type AssetCondition = 'GOOD' | 'FAIR' | 'MINOR_DAMAGE' | 'MAJOR_DAMAGE' | 'UNUSABLE'
export type CorrectionField = 'CONDITION' | 'REMARKS'

// US-CMP-06: the minimal, non-sensitive projection any authenticated user can
// fetch via GET /audits/pickable, regardless of whether they hold audits:read.
export interface AuditSummary {
  id: string
  name: string
}

export interface Audit {
  id: string
  name: string
  auditType: AuditType
  scopeOrgNodeId: string | null
  scopeOrgNodeName: string | null
  scopeCategoryId: string | null
  scopeCategoryName: string | null
  status: AuditStatus
  nominalApproverId: string
  effectiveApproverId: string | null
  submittedBy: string | null
  submittedAt: string | null
  signatureName: string | null
  approvedBy: string | null
  approvedAt: string | null
  lastRejectionReason: string | null
  scheduledDate: string | null
  version: number
}

export interface AuditProgress {
  expectedCount: number
  verifiedCount: number
  missingCount: number
  outOfScopeCount: number
  scopeChangedCount: number
  percentComplete: number
}

export interface AuditAssignment {
  id: string
  auditId: string
  auditorUserId: string
  auditorUsername: string
  subScope: string | null
  active: boolean
  unassignedAt: string | null
  version: number
}

export interface AuditFindingCorrection {
  id: string
  fieldName: CorrectionField
  oldValue: string | null
  newValue: string
  actorId: string
  actorUsername: string
  createdAt: string
}

export interface AuditFindingReconciliation {
  id: string
  findingId: string
  foundLocationNote: string
  reconciledByUserId: string
  reconciledByUsername: string
  reconciledAt: string
}

export interface AuditFinding {
  id: string
  auditId: string
  assetId: string
  assetNumber: string
  assetName: string
  status: FindingStatus
  condition: AssetCondition | null
  remarks: string | null
  verifiedByUserId: string | null
  verifiedByUsername: string | null
  verifiedAt: string
  deviceId: string | null
  scopeChangeDisposition: string | null
  corrections: AuditFindingCorrection[]
  // US-AUD-21: null unless this Missing finding has actually been reconciled.
  reconciliation: AuditFindingReconciliation | null
}

export interface AuditExceptionReport {
  auditId: string
  hasExceptions: boolean
  findings: AuditFinding[]
}

export interface AuditCertificate {
  auditId: string
  auditName: string
  expectedCount: number
  verifiedCount: number
  missingCount: number
  damagedCount: number
  approvedBy: string | null
  approvedAt: string | null
}
