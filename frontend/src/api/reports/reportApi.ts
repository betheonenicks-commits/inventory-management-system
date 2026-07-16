import { httpClient } from '../httpClient'
import type { TabularReport } from '../../features/reports/types'

// EPIC-RPT: every report shares the TabularReport shape, so one fetch + one
// CSV download function serve every report key. The CSV leg downloads via a
// blob from the same authenticated axios client - a plain <a href> would
// drop the Authorization header (tokens are memory-only by design).

export type ReportParams = Record<string, string | number | undefined>

export function fetchReport(key: string, params: ReportParams) {
  return httpClient.get<TabularReport>(`/reports/${key}`, { params }).then((r) => r.data)
}

export async function downloadReportCsv(key: string, params: ReportParams) {
  const response = await httpClient.get(`/reports/${key}`, {
    params: { ...params, format: 'csv' },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(response.data as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${key}-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const EXPORT_MIME: Record<string, string> = {
  csv: 'text/csv',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  pdf: 'application/pdf',
}

export type ExportFormat = 'csv' | 'xlsx' | 'pdf'

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** US-RPT-12: synchronous export in any format, same authed-blob path as CSV. */
export async function downloadReport(key: string, params: ReportParams, format: ExportFormat) {
  const response = await httpClient.get(`/reports/${key}`, {
    params: { ...params, format },
    responseType: 'blob',
  })
  saveBlob(response.data as Blob, `${key}-${new Date().toISOString().slice(0, 10)}.${format}`)
}

// US-RPT-12's background path: submit, poll progress, download when done.
export interface ExportJob {
  id: string
  reportKey: string
  format: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  progress: number
  fileName: string | null
  error: string | null
}

export function submitExportJob(key: string, params: ReportParams, format: ExportFormat) {
  return httpClient
    .post<ExportJob>(`/reports/${key}/export-jobs`, null, { params: { ...params, exportFormat: format } })
    .then((r) => r.data)
}

export function fetchExportJob(id: string) {
  return httpClient.get<ExportJob>(`/reports/export-jobs/${id}`).then((r) => r.data)
}

export async function downloadExportJob(job: ExportJob) {
  const response = await httpClient.get(`/reports/export-jobs/${job.id}/download`, { responseType: 'blob' })
  const blob = new Blob([response.data as Blob], { type: EXPORT_MIME[job.format] ?? 'application/octet-stream' })
  saveBlob(blob, job.fileName ?? `${job.reportKey}.${job.format}`)
}
