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
