import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createAudit,
  fetchAudit,
  fetchAuditCertificate,
  fetchAuditExceptions,
  fetchAudits,
  fetchAuditProgress,
} from '../../../api/audits/auditApi'
import type { AuditCreatePayload } from '../../../api/audits/auditApi'
import type { AuditStatus } from '../types'

export function useAuditsQuery(status?: AuditStatus) {
  return useQuery({
    queryKey: ['AUD', 'audits', status ?? null],
    queryFn: () => fetchAudits(status),
  })
}

export function useAuditQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['AUD', 'audit', id],
    queryFn: () => fetchAudit(id as string),
    enabled: !!id,
  })
}

export function useAuditProgressQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['AUD', 'progress', id],
    queryFn: () => fetchAuditProgress(id as string),
    enabled: !!id,
    // Progress changes with every scan - a short refetch-on-focus is enough,
    // no need for polling since scans in this UI always go through this tab.
    staleTime: 0,
  })
}

export function useAuditExceptionsQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['AUD', 'exceptions', id],
    queryFn: () => fetchAuditExceptions(id as string),
    enabled: !!id,
  })
}

export function useAuditCertificateQuery(id: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['AUD', 'certificate', id],
    queryFn: () => fetchAuditCertificate(id as string),
    enabled: !!id && enabled,
  })
}

export function useCreateAuditMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AuditCreatePayload) => createAudit(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AUD', 'audits'] }),
  })
}
