import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchRetentionPolicies, purgeSecurityEventLog, saveRetentionPolicy } from '../../../api/compliance/complianceApi'
import type { RetentionEntityType, RetentionExpiryAction } from '../types'

export function useRetentionPoliciesQuery() {
  return useQuery({
    queryKey: ['CMP', 'retentionPolicies'],
    queryFn: fetchRetentionPolicies,
  })
}

export function useSaveRetentionPolicyMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ entityType, retentionPeriodDays, expiryAction }: {
      entityType: RetentionEntityType
      retentionPeriodDays: number
      expiryAction: RetentionExpiryAction
    }) => saveRetentionPolicy(entityType, retentionPeriodDays, expiryAction),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'retentionPolicies'] }),
  })
}

export function usePurgeSecurityEventLogMutation() {
  return useMutation({
    mutationFn: purgeSecurityEventLog,
  })
}
