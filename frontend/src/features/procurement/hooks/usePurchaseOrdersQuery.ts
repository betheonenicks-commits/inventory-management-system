import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelPurchaseOrder,
  createPurchaseOrder,
  fetchLineEvents,
  fetchPurchaseOrder,
  fetchPurchaseOrderLines,
  fetchPurchaseOrders,
  receiveLine,
  returnLineToVendor,
} from '../../../api/procurement/procurementApi'
import type { PurchaseOrderLineInput, PurchaseOrderStatus } from '../types'

export function usePurchaseOrdersQuery(status?: PurchaseOrderStatus) {
  return useQuery({
    queryKey: ['PROC', 'purchaseOrders', status ?? null],
    queryFn: () => fetchPurchaseOrders(status),
  })
}

export function usePurchaseOrderQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['PROC', 'purchaseOrder', id],
    queryFn: () => fetchPurchaseOrder(id as string),
    enabled: !!id,
  })
}

export function usePurchaseOrderLinesQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['PROC', 'purchaseOrderLines', id],
    queryFn: () => fetchPurchaseOrderLines(id as string),
    enabled: !!id,
  })
}

export function useLineEventsQuery(lineId: string | undefined) {
  return useQuery({
    queryKey: ['PROC', 'lineEvents', lineId],
    queryFn: () => fetchLineEvents(lineId as string),
    enabled: !!lineId,
  })
}

export function useCreatePurchaseOrderMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ purchaseRequestId, vendorName, lines }: { purchaseRequestId: string; vendorName: string; lines: PurchaseOrderLineInput[] }) =>
      createPurchaseOrder(purchaseRequestId, vendorName, lines),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseOrders'] }),
  })
}

function invalidateOrder(queryClient: ReturnType<typeof useQueryClient>, id: string) {
  queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseOrder', id] })
  queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseOrderLines', id] })
  queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseOrders'] })
}

export function useCancelPurchaseOrderMutation(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reason: string) => cancelPurchaseOrder(id, reason),
    onSuccess: () => invalidateOrder(queryClient, id),
  })
}

export function useReceiveLineMutation(orderId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ lineId, quantity, discrepancyNote }: { lineId: string; quantity: number; discrepancyNote?: string }) =>
      receiveLine(lineId, quantity, discrepancyNote),
    onSuccess: (_, variables) => {
      invalidateOrder(queryClient, orderId)
      queryClient.invalidateQueries({ queryKey: ['PROC', 'lineEvents', variables.lineId] })
    },
  })
}

export function useReturnLineToVendorMutation(orderId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ lineId, quantity, reason }: { lineId: string; quantity: number; reason: string }) =>
      returnLineToVendor(lineId, quantity, reason),
    onSuccess: (_, variables) => {
      invalidateOrder(queryClient, orderId)
      queryClient.invalidateQueries({ queryKey: ['PROC', 'lineEvents', variables.lineId] })
    },
  })
}
