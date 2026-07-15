import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchActivityFeed,
  fetchAssetSummary,
  fetchAuditCalendar,
  fetchAuditCompletion,
  fetchDashboardLowStock,
  fetchDashboardPreferences,
  fetchExpirations,
  saveDashboardPreferences,
} from '../../../api/dashboard/dashboardApi'
import type { DashboardTile } from '../types'

// Each widget query is gated on its tile actually being selected (`enabled`),
// so a user who removed a tile doesn't still pay for its fetch on every load -
// the same enabled-gating discipline the approver-picker bug class established.

export function useDashboardPreferencesQuery() {
  return useQuery({ queryKey: ['DSH', 'preferences'], queryFn: fetchDashboardPreferences })
}

export function useSaveDashboardPreferencesMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (tiles: DashboardTile[]) => saveDashboardPreferences(tiles),
    onSuccess: (data) => queryClient.setQueryData(['DSH', 'preferences'], data),
  })
}

export function useAssetSummaryQuery(enabled: boolean) {
  return useQuery({ queryKey: ['DSH', 'assetSummary'], queryFn: fetchAssetSummary, enabled })
}

export function useAuditCompletionQuery(enabled: boolean) {
  return useQuery({ queryKey: ['DSH', 'auditCompletion'], queryFn: fetchAuditCompletion, enabled })
}

export function useExpirationsQuery(enabled: boolean, withinDays = 30) {
  return useQuery({
    queryKey: ['DSH', 'expirations', withinDays],
    queryFn: () => fetchExpirations(withinDays),
    enabled,
  })
}

export function useDashboardLowStockQuery(enabled: boolean) {
  return useQuery({ queryKey: ['DSH', 'lowStock'], queryFn: fetchDashboardLowStock, enabled })
}

export function useActivityFeedQuery(enabled: boolean, limit = 20) {
  return useQuery({
    queryKey: ['DSH', 'activityFeed', limit],
    queryFn: () => fetchActivityFeed(limit),
    enabled,
  })
}

export function useAuditCalendarQuery(enabled: boolean, withinDays = 30) {
  return useQuery({
    queryKey: ['DSH', 'auditCalendar', withinDays],
    queryFn: () => fetchAuditCalendar(withinDays),
    enabled,
  })
}
