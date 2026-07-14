import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { assignAuditor, fetchAuditAssignments, unassignAuditor } from '../../../api/audits/auditApi'

export function useAuditAssignmentsQuery(auditId: string | undefined) {
  return useQuery({
    queryKey: ['AUD', 'assignments', auditId],
    queryFn: () => fetchAuditAssignments(auditId as string),
    enabled: !!auditId,
  })
}

export function useAssignAuditorMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ auditorUserId, subScope }: { auditorUserId: string; subScope?: string }) =>
      assignAuditor(auditId, auditorUserId, subScope),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AUD', 'assignments', auditId] }),
  })
}

export function useUnassignAuditorMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (assignmentId: string) => unassignAuditor(auditId, assignmentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AUD', 'assignments', auditId] }),
  })
}
