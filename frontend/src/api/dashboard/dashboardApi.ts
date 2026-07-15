import { httpClient } from '../httpClient'
import type {
  ActivityFeedEntry,
  AssetSummary,
  AuditCalendarEntry,
  AuditCompletion,
  DashboardLowStockItem,
  DashboardPreferences,
  DashboardTile,
  Expiration,
} from '../../features/dashboard/types'

// EPIC-DSH: every endpoint rides on the single dashboards:read permission -
// see DashboardController's Javadoc for the aggregate-vs-detail reasoning.

export function fetchAssetSummary() {
  return httpClient.get<AssetSummary>('/dashboard/asset-summary').then((r) => r.data)
}

export function fetchAuditCompletion() {
  return httpClient.get<AuditCompletion>('/dashboard/audit-completion').then((r) => r.data)
}

export function fetchExpirations(withinDays = 30) {
  return httpClient.get<Expiration[]>('/dashboard/expirations', { params: { withinDays } }).then((r) => r.data)
}

export function fetchDashboardLowStock() {
  return httpClient.get<DashboardLowStockItem[]>('/dashboard/low-stock').then((r) => r.data)
}

export function fetchActivityFeed(limit = 20) {
  return httpClient.get<ActivityFeedEntry[]>('/dashboard/activity-feed', { params: { limit } }).then((r) => r.data)
}

export function fetchAuditCalendar(withinDays = 30) {
  return httpClient.get<AuditCalendarEntry[]>('/dashboard/audit-calendar', { params: { withinDays } }).then((r) => r.data)
}

export function fetchDashboardPreferences() {
  return httpClient.get<DashboardPreferences>('/dashboard/preferences').then((r) => r.data)
}

export function saveDashboardPreferences(tiles: DashboardTile[]) {
  return httpClient.put<DashboardPreferences>('/dashboard/preferences', { tiles }).then((r) => r.data)
}
