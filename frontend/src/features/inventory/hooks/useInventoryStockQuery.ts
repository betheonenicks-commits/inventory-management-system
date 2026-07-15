import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchBalances,
  fetchExpiringLots,
  fetchLowStock,
  fetchTransactions,
  stockIn,
  stockOut,
  transferStock,
} from '../../../api/inventory/inventoryApi'
import type { StockInPayload, StockOutPayload, StockTransferPayload } from '../../../api/inventory/inventoryApi'

export function useBalancesQuery(itemId: string | undefined, warehouseId?: string) {
  return useQuery({
    queryKey: ['INV', 'balances', itemId, warehouseId ?? null],
    queryFn: () => fetchBalances(itemId, warehouseId),
    enabled: !!itemId,
  })
}

export function useTransactionsQuery(itemId: string | undefined) {
  return useQuery({
    queryKey: ['INV', 'transactions', itemId],
    queryFn: () => fetchTransactions(itemId as string),
    enabled: !!itemId,
  })
}

export function useLowStockQuery(enabled = true) {
  return useQuery({ queryKey: ['INV', 'lowStock'], queryFn: fetchLowStock, enabled })
}

export function useExpiringLotsQuery(withinDays: number, enabled = true) {
  return useQuery({ queryKey: ['INV', 'expiringLots', withinDays], queryFn: () => fetchExpiringLots(withinDays), enabled })
}

function invalidateStockData(queryClient: ReturnType<typeof useQueryClient>, itemId: string) {
  queryClient.invalidateQueries({ queryKey: ['INV', 'balances', itemId] })
  queryClient.invalidateQueries({ queryKey: ['INV', 'transactions', itemId] })
  queryClient.invalidateQueries({ queryKey: ['INV', 'lowStock'] })
  queryClient.invalidateQueries({ queryKey: ['INV', 'expiringLots'] })
}

export function useStockInMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: StockInPayload) => stockIn(payload),
    onSuccess: (_, payload) => invalidateStockData(queryClient, payload.itemId),
  })
}

export function useStockOutMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: StockOutPayload) => stockOut(payload),
    onSuccess: (_, payload) => invalidateStockData(queryClient, payload.itemId),
  })
}

export function useTransferStockMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: StockTransferPayload) => transferStock(payload),
    onSuccess: (_, payload) => invalidateStockData(queryClient, payload.itemId),
  })
}
