import { useMutation, useQueryClient } from '@tanstack/react-query'
import { approveAudit, rejectAudit, submitAudit } from '../../../api/audits/auditApi'

// Submit auto-classifies every not-yet-verified expected asset as Missing
// (US-AUD-09) and reject undoes those system-classified Missing findings
// (per AuditWorkflowService.reject) - both change progress/exceptions data,
// not just the audit's own status, so both must be invalidated too or the
// UI shows stale counts until an unrelated refetch happens to occur.
function invalidateAuditAndDerivedData(queryClient: ReturnType<typeof useQueryClient>, auditId: string) {
  queryClient.invalidateQueries({ queryKey: ['AUD', 'audit', auditId] })
  queryClient.invalidateQueries({ queryKey: ['AUD', 'audits'] })
  queryClient.invalidateQueries({ queryKey: ['AUD', 'progress', auditId] })
  queryClient.invalidateQueries({ queryKey: ['AUD', 'exceptions', auditId] })
}

export function useSubmitAuditMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ password, signatureName }: { password: string; signatureName?: string }) =>
      submitAudit(auditId, password, signatureName),
    onSuccess: () => invalidateAuditAndDerivedData(queryClient, auditId),
  })
}

export function useApproveAuditMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => approveAudit(auditId),
    onSuccess: () => invalidateAuditAndDerivedData(queryClient, auditId),
  })
}

export function useRejectAuditMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reason: string) => rejectAudit(auditId, reason),
    onSuccess: () => invalidateAuditAndDerivedData(queryClient, auditId),
  })
}
