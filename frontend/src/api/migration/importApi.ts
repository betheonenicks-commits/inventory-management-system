import { httpClient } from '../httpClient'

// EPIC-MIG: bulk data import. Two permissions gate this feature (mirroring the
// backend): imports:write runs an import (template/dry-run/commit); imports:read
// browses history. The multipart upload and the blob template download both go
// through the authenticated axios client - a plain <a href> would drop the
// Authorization header (tokens are memory-only by design).

export type ImportEntityType = 'ASSET' | 'PERSON' | 'VENDOR' | 'INVENTORY_ITEM'
export type ImportRunStatus = 'VALIDATED' | 'COMMITTED' | 'FAILED'

// The entity types with an executable importer today; the rest are declared so
// the selector is stable as they're built, and disabled in the UI until then.
export const EXECUTABLE_ENTITY_TYPES: ImportEntityType[] = ['ASSET', 'VENDOR']

export interface ImportRowError {
  rowNumber: number
  field: string
  message: string
}

export interface ImportRun {
  id: string
  entityType: ImportEntityType
  status: ImportRunStatus
  templateVersion: string | null
  originalFilename: string | null
  totalRows: number
  validRows: number
  invalidRows: number
  committedRows: number | null
  failedRows: number | null
  skippedRows: number | null
  outcome: string | null
  errorReport: ImportRowError[]
  submittedBy: string
  submittedAt: string
  committedBy: string | null
  committedAt: string | null
}

export type ImportRunSummary = Omit<ImportRun, 'errorReport' | 'templateVersion'>

/** US-MIG-01: download the versioned template for an entity type as a CSV file. */
export async function downloadImportTemplate(entityType: ImportEntityType) {
  const response = await httpClient.get(`/imports/templates/${entityType}`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${entityType.toLowerCase()}-import-template.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** US-MIG-03: dry-run validate an uploaded file, returning the per-row error report. Nothing is written. */
export function dryRunImport(entityType: ImportEntityType, file: File) {
  const form = new FormData()
  form.append('file', file)
  return httpClient.post<ImportRun>(`/imports/dry-run/${entityType}`, form).then((r) => r.data)
}

/** US-MIG-03: commit the rows that passed dry-run, idempotently. */
export function commitImport(runId: string, idempotencyKey: string) {
  return httpClient.post<ImportRun>(`/imports/${runId}/commit`, { idempotencyKey }).then((r) => r.data)
}

export function fetchImportRun(runId: string) {
  return httpClient.get<ImportRun>(`/imports/${runId}`).then((r) => r.data)
}

/** US-MIG-04: import run history (imports:read - Super Admin/Admin/IT Security Officer). */
export function fetchImportHistory() {
  return httpClient.get<ImportRunSummary[]>('/imports').then((r) => r.data)
}
