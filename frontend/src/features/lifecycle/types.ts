// Mirrors backend/src/main/java/com/iams/lifecycle/api/dto/*.java, com.iams/maintenance/api/dto/*.java, and their domain enums.

export type LifecycleRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type DisposalType = 'RETIRE' | 'DISPOSE' | 'DONATE'
// US-AST-04: how a component (child) asset is handled when its parent is transferred/disposed.
export type ChildDisposition = 'MOVE_WITH_PARENT' | 'DETACH'
export type RepairEventStatus = 'OPEN' | 'CLOSED'
export type MaintenanceType = 'PREVENTIVE' | 'CORRECTIVE'

export interface Transfer {
  id: string
  version: number
  assetId: string
  assetNumber: string
  fromOrgNodeId: string | null
  fromOrgNodeCode: string | null
  toOrgNodeId: string
  toOrgNodeCode: string
  fromPersonId: string | null
  toPersonId: string | null
  reason: string
  status: LifecycleRequestStatus
  nominalApproverId: string
  effectiveApproverId: string | null
  requestedBy: string
  requestedAt: string
  decidedBy: string | null
  decidedAt: string | null
  rejectionReason: string | null
}

export interface Disposal {
  id: string
  version: number
  assetId: string
  assetNumber: string
  disposalType: DisposalType
  reason: string
  status: LifecycleRequestStatus
  nominalApproverId: string
  effectiveApproverId: string | null
  requestedBy: string
  requestedAt: string
  decidedBy: string | null
  decidedAt: string | null
  rejectionReason: string | null
  restoredAt: string | null
  restoredBy: string | null
}

export interface RepairEvent {
  id: string
  version: number
  assetId: string
  assetNumber: string
  previousStatusCode: string
  vendorName: string | null
  reason: string
  estimatedCost: number | null
  expectedReturnDate: string | null
  actualCost: number | null
  actualReturnDate: string | null
  status: RepairEventStatus
  loggedBy: string
}

export interface MaintenanceSchedule {
  id: string
  version: number
  assetId: string
  assetNumber: string
  intervalMonths: number
  nextDueDate: string
  description: string | null
  active: boolean
}

export interface MaintenanceEvent {
  id: string
  version: number
  assetId: string
  assetNumber: string
  scheduleId: string | null
  maintenanceType: MaintenanceType
  performedAt: string
  notes: string | null
  cost: number | null
  performedBy: string
}
