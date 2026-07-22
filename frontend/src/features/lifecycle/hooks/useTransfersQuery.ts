import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approveTransfer,
  createTransfer,
  escalateTransfer,
  fetchTransfers,
  rejectTransfer,
} from '../../../api/lifecycle/lifecycleApi'
import type { ChildDisposition } from '../types'

export function useTransfersQuery(assetId?: string) {
  return useQuery({
    queryKey: ['LIF', 'transfers', assetId ?? null],
    queryFn: () => fetchTransfers(assetId),
    enabled: !!assetId,
  })
}

function invalidate(queryClient: ReturnType<typeof useQueryClient>, assetId: string) {
  queryClient.invalidateQueries({ queryKey: ['LIF', 'transfers', assetId] })
  queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
}

export function useCreateTransferMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ toOrgNodeId, toPersonId, reason, nominalApproverId, childDispositions }: {
      toOrgNodeId: string
      toPersonId?: string
      reason: string
      nominalApproverId: string
      childDispositions?: Record<string, ChildDisposition>
    }) => createTransfer(assetId, toOrgNodeId, toPersonId, reason, nominalApproverId, childDispositions),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useApproveTransferMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => approveTransfer(id),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useRejectTransferMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => rejectTransfer(id, reason),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useEscalateTransferMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => escalateTransfer(id),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}
