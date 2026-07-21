import { useMutation, useQueryClient } from '@tanstack/react-query'
import { recordBatchScan, recordScan } from '../../../api/audits/auditApi'
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

// US-AUD-07: batch-scan's frontend caller - previously the endpoint existed with no UI to reach it.
export function useRecordBatchScanMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (scans: AuditScanPayload[]) => recordBatchScan(auditId, scans),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AUD', 'progress', auditId] })
      queryClient.invalidateQueries({ queryKey: ['AUD', 'exceptions', auditId] })
    },
  })
}
