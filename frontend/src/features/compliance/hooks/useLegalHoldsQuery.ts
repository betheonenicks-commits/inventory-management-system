import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchLegalHolds, liftLegalHold, placeLegalHold } from '../../../api/compliance/complianceApi'
import type { LegalHoldScopeType } from '../types'

export function useLegalHoldsQuery(scopeType?: LegalHoldScopeType) {
  return useQuery({
    queryKey: ['CMP', 'legalHolds', scopeType ?? null],
    queryFn: () => fetchLegalHolds(scopeType),
  })
}

export function usePlaceLegalHoldMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ scopeType, scopeId, reason }: { scopeType: LegalHoldScopeType; scopeId: string; reason: string }) =>
      placeLegalHold(scopeType, scopeId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'legalHolds'] }),
  })
}

export function useLiftLegalHoldMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, liftReason }: { id: string; liftReason: string }) => liftLegalHold(id, liftReason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'legalHolds'] }),
  })
}
