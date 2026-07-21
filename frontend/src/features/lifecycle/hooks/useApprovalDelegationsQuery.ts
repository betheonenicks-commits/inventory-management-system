import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMe } from '../../../api/authApi'
import {
  createApprovalDelegation,
  fetchApprovalDelegations,
  revokeApprovalDelegation,
} from '../../../api/lifecycle/lifecycleApi'

// The stored session never carries the user's own id - US-LIF-15's delegation
// list/create needs delegatorUserId, so this resolves it once via /auth/me.
export function useMeQuery() {
  return useQuery({ queryKey: ['auth', 'me'], queryFn: fetchMe, staleTime: 5 * 60 * 1000 })
}

export function useApprovalDelegationsQuery(delegatorUserId: string | undefined) {
  return useQuery({
    queryKey: ['LIF', 'delegations', delegatorUserId ?? null],
    queryFn: () => fetchApprovalDelegations(delegatorUserId as string),
    enabled: !!delegatorUserId,
  })
}

export function useCreateApprovalDelegationMutation(delegatorUserId: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ delegateUserId, validFrom, validTo, reason }: {
      delegateUserId: string
      validFrom: string
      validTo: string
      reason?: string
    }) => createApprovalDelegation(delegateUserId, validFrom, validTo, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['LIF', 'delegations', delegatorUserId ?? null] }),
  })
}

export function useRevokeApprovalDelegationMutation(delegatorUserId: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => revokeApprovalDelegation(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['LIF', 'delegations', delegatorUserId ?? null] }),
  })
}
