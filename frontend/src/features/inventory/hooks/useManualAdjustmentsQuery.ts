import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approveManualAdjustment,
  fetchManualAdjustment,
  fetchManualAdjustments,
  rejectManualAdjustment,
  requestManualAdjustment,
} from '../../../api/inventory/inventoryApi'
import type { LifecycleRequestStatus } from '../../lifecycle/types'

export function useManualAdjustmentsQuery(status?: LifecycleRequestStatus) {
  return useQuery({
    queryKey: ['INV', 'adjustments', status ?? null],
    queryFn: () => fetchManualAdjustments(status),
  })
}

export function useManualAdjustmentQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['INV', 'adjustment', id],
    queryFn: () => fetchManualAdjustment(id as string),
    enabled: !!id,
  })
}

export function useRequestManualAdjustmentMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: {
      itemId: string
      warehouseId: string
      quantityDelta: number
      reason: string
      nominalApproverId: string
      subLocation?: string
      lotNumber?: string
    }) =>
      requestManualAdjustment(
        payload.itemId,
        payload.warehouseId,
        payload.quantityDelta,
        payload.reason,
        payload.nominalApproverId,
        payload.subLocation,
        payload.lotNumber,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'adjustments'] }),
  })
}

function invalidateAdjustment(queryClient: ReturnType<typeof useQueryClient>, id: string) {
  queryClient.invalidateQueries({ queryKey: ['INV', 'adjustments'] })
  queryClient.invalidateQueries({ queryKey: ['INV', 'adjustment', id] })
  queryClient.invalidateQueries({ queryKey: ['INV', 'balances'] })
  queryClient.invalidateQueries({ queryKey: ['INV', 'lowStock'] })
}

export function useApproveManualAdjustmentMutation(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => approveManualAdjustment(id),
    onSuccess: () => invalidateAdjustment(queryClient, id),
  })
}

export function useRejectManualAdjustmentMutation(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reason: string) => rejectManualAdjustment(id, reason),
    onSuccess: () => invalidateAdjustment(queryClient, id),
  })
}
