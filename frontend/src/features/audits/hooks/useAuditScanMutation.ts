import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recordScan } from '../../../api/audits/auditApi'
import type { AuditScanPayload } from '../../../api/audits/auditApi'

export function useRecordScanMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AuditScanPayload) => recordScan(auditId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AUD', 'progress', auditId] })
      queryClient.invalidateQueries({ queryKey: ['AUD', 'exceptions', auditId] })
    },
  })
}
