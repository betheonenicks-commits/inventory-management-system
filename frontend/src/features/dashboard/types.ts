// EPIC-DSH types, mirrored 1:1 against DashboardDtos.java (read fresh, not assumed).

export type DashboardTile =
  | 'ASSET_SUMMARY'
  | 'AUDIT_COMPLETION'
  | 'EXPIRATIONS'
  | 'LOW_STOCK'
  | 'ACTIVITY_FEED'
  | 'AUDIT_CALENDAR'

export interface LabelCount {
  label: string
  count: number
}

export interface AssetSummary {
  totalAssets: number
  byCategory: LabelCount[]
  byStatus: LabelCount[]
}

export interface AuditCompletionItem {
  auditId: string
  name: string
  status: string
  percentComplete: number
  exceptionCount: number
}

export interface AuditCompletion {
  audits: AuditCompletionItem[]
  // null = no active audits in scope (the AC-DSH-02 empty state), never 0
  averagePercentComplete: number | null
  // US-AUD-17: the "recent" half of the dashboard - most-recently-closed audits
  recentlyClosed: AuditCompletionItem[]
}

export type ExpirationKind = 'WARRANTY' | 'INSURANCE' | 'MAINTENANCE'

export interface Expiration {
  kind: ExpirationKind
  assetId: string
  assetName: string
  dueDate: string
  detail: string | null
}

export interface DashboardLowStockItem {
  itemId: string
  name: string
  sku: string
  unitOfMeasure: string
  totalQuantity: number
  reorderLevel: number
}

export interface ActivityFeedEntry {
  eventId: string
  eventType: string
  assetId: string
  assetName: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  actorId: string
  occurredAt: string
}

export interface AuditCalendarEntry {
  auditId: string
  name: string
  status: string
  scheduledDate: string
}

export interface DashboardPreferences {
  tiles: DashboardTile[]
  configured: boolean
  availableTiles: DashboardTile[]
}
