import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createInventoryItem,
  deactivateInventoryItem,
  fetchCostingMethodHistory,
  fetchInventoryItem,
  fetchInventoryItems,
  updateInventoryItem,
} from '../../../api/inventory/inventoryApi'
import type { CostingMethod, UnitOfMeasure } from '../types'

export function useInventoryItemsQuery(activeOnly = false, enabled = true) {
  return useQuery({ queryKey: ['INV', 'items', activeOnly], queryFn: () => fetchInventoryItems(activeOnly), enabled })
}

export function useInventoryItemQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['INV', 'item', id],
    queryFn: () => fetchInventoryItem(id as string),
    enabled: !!id,
  })
}

export function useCostingMethodHistoryQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['INV', 'costingMethodHistory', id],
    queryFn: () => fetchCostingMethodHistory(id as string),
    enabled: !!id,
  })
}

export function useCreateInventoryItemMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { name: string; sku: string; unitOfMeasure: UnitOfMeasure; reorderLevel?: number; costingMethod?: CostingMethod }) =>
      createInventoryItem(payload.name, payload.sku, payload.unitOfMeasure, payload.reorderLevel, payload.costingMethod),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'items'] }),
  })
}

export function useUpdateInventoryItemMutation(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { name: string; reorderLevel?: number; costingMethod?: CostingMethod }) =>
      updateInventoryItem(id, payload.name, payload.reorderLevel, payload.costingMethod),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['INV', 'items'] })
      queryClient.invalidateQueries({ queryKey: ['INV', 'item', id] })
      queryClient.invalidateQueries({ queryKey: ['INV', 'costingMethodHistory', id] })
    },
  })
}

export function useDeactivateInventoryItemMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deactivateInventoryItem(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'items'] }),
  })
}
