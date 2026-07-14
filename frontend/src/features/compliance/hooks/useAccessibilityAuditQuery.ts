import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAccessibilityAudit, recordAccessibilityAudit } from '../../../api/compliance/complianceApi'
import type { AccessibilityAuditOutcome } from '../types'

export function useAccessibilityAuditQuery() {
  return useQuery({
    queryKey: ['CMP', 'accessibilityAudit'],
    queryFn: fetchAccessibilityAudit,
  })
}

export function useRecordAccessibilityAuditMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ auditDate, outcome, notes }: { auditDate: string; outcome: AccessibilityAuditOutcome; notes?: string }) =>
      recordAccessibilityAudit(auditDate, outcome, notes),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'accessibilityAudit'] }),
  })
}
