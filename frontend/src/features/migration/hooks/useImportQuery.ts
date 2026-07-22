import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  commitImport,
  dryRunImport,
  fetchImportHistory,
  type ImportEntityType,
} from '../../../api/migration/importApi'

const HISTORY_KEY = ['MIG', 'importHistory']

/** US-MIG-04: import run history. Only enabled when the caller holds imports:read (an Inventory Manager who runs imports does not, and would 403). */
export function useImportHistoryQuery(enabled: boolean) {
  return useQuery({
    queryKey: HISTORY_KEY,
    queryFn: fetchImportHistory,
    enabled,
  })
}

export function useDryRunMutation() {
  return useMutation({
    mutationFn: ({ entityType, file }: { entityType: ImportEntityType; file: File }) => dryRunImport(entityType, file),
  })
}

export function useCommitMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ runId, idempotencyKey }: { runId: string; idempotencyKey: string }) =>
      commitImport(runId, idempotencyKey),
    // A commit creates assets and adds a run to history.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: HISTORY_KEY }),
  })
}
