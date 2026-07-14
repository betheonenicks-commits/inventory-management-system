import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approvePurchaseRequest,
  createPurchaseRequest,
  fetchPurchaseRequest,
  fetchPurchaseRequests,
  rejectPurchaseRequest,
} from '../../../api/procurement/procurementApi'
import type { LifecycleRequestStatus } from '../../lifecycle/types'

export function usePurchaseRequestsQuery(status?: LifecycleRequestStatus) {
  return useQuery({
    queryKey: ['PROC', 'purchaseRequests', status ?? null],
    queryFn: () => fetchPurchaseRequests(status),
  })
}

export function usePurchaseRequestQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['PROC', 'purchaseRequest', id],
    queryFn: () => fetchPurchaseRequest(id as string),
    enabled: !!id,
  })
}

export function useCreatePurchaseRequestMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ itemDescription, justification, estimatedCost, vendorName, nominalApproverId }: {
      itemDescription: string
      justification: string
      estimatedCost?: number
      vendorName?: string
      nominalApproverId: string
    }) => createPurchaseRequest(itemDescription, justification, estimatedCost, vendorName, nominalApproverId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseRequests'] }),
  })
}

export function useApprovePurchaseRequestMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => approvePurchaseRequest(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseRequests'] }),
  })
}

export function useRejectPurchaseRequestMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => rejectPurchaseRequest(id, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['PROC', 'purchaseRequests'] }),
  })
}
