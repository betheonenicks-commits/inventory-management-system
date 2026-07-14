import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  closeRepair,
  createMaintenanceSchedule,
  deactivateMaintenanceSchedule,
  fetchMaintenanceEvents,
  fetchMaintenanceSchedules,
  fetchRepairs,
  openRepair,
  recordCorrectiveMaintenance,
  recordPreventiveMaintenance,
} from '../../../api/maintenance/maintenanceApi'

function invalidateAssetMaintenance(queryClient: ReturnType<typeof useQueryClient>, assetId: string) {
  queryClient.invalidateQueries({ queryKey: ['LIF', 'repairs', assetId] })
  queryClient.invalidateQueries({ queryKey: ['LIF', 'schedules', assetId] })
  queryClient.invalidateQueries({ queryKey: ['LIF', 'events', assetId] })
  queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
}

// --- Repairs ---

export function useRepairsQuery(assetId?: string) {
  return useQuery({
    queryKey: ['LIF', 'repairs', assetId ?? null],
    queryFn: () => fetchRepairs(assetId),
    enabled: !!assetId,
  })
}

export function useOpenRepairMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ vendorName, reason, estimatedCost, expectedReturnDate }: {
      vendorName?: string
      reason: string
      estimatedCost?: number
      expectedReturnDate?: string
    }) => openRepair(assetId, vendorName, reason, estimatedCost, expectedReturnDate),
    onSuccess: () => invalidateAssetMaintenance(queryClient, assetId),
  })
}

export function useCloseRepairMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, actualReturnDate, actualCost }: { id: string; actualReturnDate: string; actualCost?: number }) =>
      closeRepair(id, actualReturnDate, actualCost),
    onSuccess: () => invalidateAssetMaintenance(queryClient, assetId),
  })
}

// --- Preventive schedules ---

export function useMaintenanceSchedulesQuery(assetId?: string) {
  return useQuery({
    queryKey: ['LIF', 'schedules', assetId ?? null],
    queryFn: () => fetchMaintenanceSchedules(assetId),
    enabled: !!assetId,
  })
}

export function useCreateMaintenanceScheduleMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ intervalMonths, nextDueDate, description }: { intervalMonths: number; nextDueDate: string; description?: string }) =>
      createMaintenanceSchedule(assetId, intervalMonths, nextDueDate, description),
    onSuccess: () => invalidateAssetMaintenance(queryClient, assetId),
  })
}

export function useDeactivateMaintenanceScheduleMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deactivateMaintenanceSchedule(id),
    onSuccess: () => invalidateAssetMaintenance(queryClient, assetId),
  })
}

// --- Maintenance events ---

export function useMaintenanceEventsQuery(assetId?: string) {
  return useQuery({
    queryKey: ['LIF', 'events', assetId ?? null],
    queryFn: () => fetchMaintenanceEvents(assetId),
    enabled: !!assetId,
  })
}

export function useRecordPreventiveMaintenanceMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ scheduleId, notes, cost }: { scheduleId: string; notes?: string; cost?: number }) =>
      recordPreventiveMaintenance(scheduleId, notes, cost),
    onSuccess: () => invalidateAssetMaintenance(queryClient, assetId),
  })
}

export function useRecordCorrectiveMaintenanceMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ notes, cost }: { notes: string; cost?: number }) => recordCorrectiveMaintenance(assetId, notes, cost),
    onSuccess: () => invalidateAssetMaintenance(queryClient, assetId),
  })
}
