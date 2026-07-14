import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approveDisposal,
  createDisposal,
  escalateDisposal,
  fetchDisposals,
  rejectDisposal,
  restoreDisposal,
} from '../../../api/lifecycle/lifecycleApi'
import type { DisposalType } from '../types'

export function useDisposalsQuery(assetId?: string) {
  return useQuery({
    queryKey: ['LIF', 'disposals', assetId ?? null],
    queryFn: () => fetchDisposals(assetId),
    enabled: !!assetId,
  })
}

function invalidate(queryClient: ReturnType<typeof useQueryClient>, assetId: string) {
  queryClient.invalidateQueries({ queryKey: ['LIF', 'disposals', assetId] })
  queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
}

export function useCreateDisposalMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ disposalType, reason, nominalApproverId }: { disposalType: DisposalType; reason: string; nominalApproverId: string }) =>
      createDisposal(assetId, disposalType, reason, nominalApproverId),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useApproveDisposalMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => approveDisposal(id),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useRejectDisposalMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => rejectDisposal(id, reason),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useRestoreDisposalMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => restoreDisposal(id),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}

export function useEscalateDisposalMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => escalateDisposal(id),
    onSuccess: () => invalidate(queryClient, assetId),
  })
}
