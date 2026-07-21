import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  correctFinding,
  createAudit,
  fetchAudit,
  fetchAuditCertificate,
  fetchAuditExceptions,
  fetchAudits,
  fetchAuditProgress,
  fetchCrossCycleTrends,
  fetchFindingEvidence,
  fetchPickableAudits,
  reconcileFinding,
  resolveScopeChange,
  uploadFindingEvidence,
} from '../../../api/audits/auditApi'
import type { AuditCreatePayload } from '../../../api/audits/auditApi'
import type { AuditStatus, CorrectionField, ScopeChangeDisposition } from '../types'

export function useAuditsQuery(status?: AuditStatus, enabled = true) {
  return useQuery({
    queryKey: ['AUD', 'audits', status ?? null],
    queryFn: () => fetchAudits(status),
    enabled,
  })
}

export function useAuditQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['AUD', 'audit', id],
    queryFn: () => fetchAudit(id as string),
    enabled: !!id,
  })
}

// US-AUD-18: cross-cycle analytics; enabled-gated so it only fetches when the panel is shown.
export function useCrossCycleTrendsQuery(enabled = true) {
  return useQuery({
    queryKey: ['AUD', 'cross-cycle-trends'],
    queryFn: fetchCrossCycleTrends,
    enabled,
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

/** US-CMP-06: for pickers outside EPIC-AUD itself - any authenticated user, not just audits:read holders. */
export function usePickableAuditsQuery(enabled = true) {
  return useQuery({
    queryKey: ['AUD', 'pickable'],
    queryFn: fetchPickableAudits,
    enabled,
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

/** US-AUD-21: reconciles a Missing finding - the original finding is never edited, so only the exceptions list (which now embeds the new reconciliation) needs refetching. */
export function useReconcileFindingMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ findingId, foundLocationNote }: { findingId: string; foundLocationNote: string }) =>
      reconcileFinding(auditId, findingId, foundLocationNote),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AUD', 'exceptions', auditId] }),
  })
}

// US-AUD-24: the corrections endpoint's frontend caller - previously it existed with no UI mutation reaching it.
export function useCorrectFindingMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      findingId,
      fieldName,
      newValue,
    }: {
      findingId: string
      fieldName: CorrectionField
      newValue: string
    }) => correctFinding(auditId, findingId, fieldName, newValue),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AUD', 'exceptions', auditId] }),
  })
}

// US-AUD-23: the scope-change-disposition endpoint's frontend caller - previously it
// existed with no way in the product to ever set it, permanently blocking closure.
export function useResolveScopeChangeMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ findingId, disposition }: { findingId: string; disposition: ScopeChangeDisposition }) =>
      resolveScopeChange(auditId, findingId, disposition),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AUD', 'exceptions', auditId] }),
  })
}

// US-AUD-11: evidence list per finding - cached per finding id, invalidated on upload.
export function useFindingEvidenceQuery(auditId: string, findingId: string) {
  return useQuery({
    queryKey: ['AUD', 'evidence', auditId, findingId],
    queryFn: () => fetchFindingEvidence(auditId, findingId),
  })
}

export function useUploadEvidenceMutation(auditId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ findingId, file }: { findingId: string; file: File }) =>
      uploadFindingEvidence(auditId, findingId, file),
    onSuccess: (_data, { findingId }) =>
      queryClient.invalidateQueries({ queryKey: ['AUD', 'evidence', auditId, findingId] }),
  })
}
