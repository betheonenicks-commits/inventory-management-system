import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import DownloadIcon from '@mui/icons-material/Download'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { PageHeader } from '../../components/common/PageHeader'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import {
  downloadExportJob,
  downloadReport,
  fetchExportJob,
  fetchReport,
  submitExportJob,
} from '../../api/reports/reportApi'
import type { ExportFormat, ExportJob, ReportParams } from '../../api/reports/reportApi'
import LinearProgress from '@mui/material/LinearProgress'
import { fetchOrgNodes } from '../../api/org/orgNodeApi'
import { fetchPersons } from '../../api/persons/personApi'
import type { TabularReport } from './types'

type ReportKey =
  | 'asset-register'
  | 'employee-assets'
  | 'expiry'
  | 'asset-movements'
  | 'loss'
  | 'vendor-purchases'
  | 'audit-compliance'
  | 'depreciation'
  | 'maintenance-history'
  | 'security-events'

interface ReportDef {
  key: ReportKey
  label: string
  description: string
  // security-events is security:read-gated (US-RPT-14's AC refuses a Viewer);
  // everything else rides on reports:read, which is what gates this page.
  requiresPermission?: string
}

const REPORTS: ReportDef[] = [
  { key: 'asset-register', label: 'Asset Register', description: 'Every in-scope asset with its key attributes (US-RPT-01/02).' },
  { key: 'employee-assets', label: 'Employee Asset List', description: 'Everything assigned to one person, with assignment dates (US-RPT-03).' },
  { key: 'expiry', label: 'Expiring Coverage & Maintenance', description: 'Warranty/AMC/insurance expiry and maintenance due in a lookahead window (US-RPT-05).' },
  { key: 'asset-movements', label: 'Asset Movements', description: 'Relocations between locations over a date range (US-RPT-07).' },
  { key: 'loss', label: 'Missing / Lost / Damaged', description: 'Every Missing or damaged finding across all audits, with its source audit (US-RPT-04).' },
  { key: 'vendor-purchases', label: 'Purchase & Vendor', description: 'Item-level PO detail with per-vendor subtotals and a grand total (US-RPT-06).' },
  { key: 'audit-compliance', label: 'Audit Compliance Summary', description: 'Completion, exception, and on-time rates across a period (US-RPT-08).' },
  { key: 'depreciation', label: 'Depreciation & Net Book Value', description: 'NBV per asset from its stored schedule; unconfigured assets flagged, never zeroed (US-RPT-09).' },
  { key: 'maintenance-history', label: 'Maintenance History', description: 'Repairs plus preventive/corrective maintenance in one timeline (US-RPT-10).' },
  { key: 'security-events', label: 'Security & Access Log', description: 'The security log as a formal, exportable report (US-RPT-14).', requiresPermission: 'security:read' },
]

// Reports whose only controls are an optional from/to date range.
const DATE_RANGE_REPORTS: ReportKey[] = ['asset-movements', 'loss', 'vendor-purchases', 'audit-compliance']

function defaultFrom() {
  const d = new Date()
  d.setDate(d.getDate() - 30)
  return d.toISOString().slice(0, 10)
}

/** EPIC-RPT: one generic page renders every report - the uniform TabularReport shape is the whole point (see TabularReport.java). */
export function ReportsPage() {
  const user = useAuthStore((s) => s.user)
  const visibleReports = REPORTS.filter((r) => !r.requiresPermission || hasPermission(user, r.requiresPermission))

  const [reportKey, setReportKey] = useState<ReportKey>('asset-register')
  const [orgNodeId, setOrgNodeId] = useState('')
  const [personId, setPersonId] = useState('')
  const [withinDays, setWithinDays] = useState('90')
  const [from, setFrom] = useState(defaultFrom())
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10))
  const [asOf, setAsOf] = useState(new Date().toISOString().slice(0, 10))
  const [report, setReport] = useState<TabularReport | null>(null)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exportFormat, setExportFormat] = useState<ExportFormat>('xlsx')
  const [exportJob, setExportJob] = useState<ExportJob | null>(null)

  // Pickers fetch only while their owning report is selected - the enabled-gating
  // discipline every prior permission bug in this codebase taught.
  const orgNodesQuery = useQuery({
    queryKey: ['ORG', 'orgNodes'],
    queryFn: fetchOrgNodes,
    enabled: reportKey === 'asset-register',
    staleTime: 5 * 60 * 1000,
  })
  const personsQuery = useQuery({
    queryKey: ['RPT', 'persons'],
    queryFn: () => fetchPersons(),
    enabled: reportKey === 'employee-assets',
  })

  function currentParams(): ReportParams | { error: string } {
    switch (reportKey) {
      case 'asset-register':
        return orgNodeId ? { orgNodeId } : {}
      case 'employee-assets':
        return personId ? { personId } : { error: 'Choose a person first.' }
      case 'expiry':
        return { withinDays: Number(withinDays) }
      case 'asset-movements':
        return { from, to }
      case 'loss':
      case 'vendor-purchases':
      case 'audit-compliance':
        // Optional range - blank inputs mean all-time.
        return { from: from || undefined, to: to || undefined }
      case 'depreciation':
        return asOf ? { asOf } : {}
      case 'maintenance-history':
        return {}
      case 'security-events':
        return {}
    }
  }

  async function run() {
    const params = currentParams()
    if ('error' in params) {
      setError(params.error as string)
      return
    }
    setError(null)
    setRunning(true)
    try {
      setReport(await fetchReport(reportKey, params))
    } catch (err) {
      setReport(null)
      setError(isApiProblem(err) ? err.detail : 'Failed to generate the report')
    } finally {
      setRunning(false)
    }
  }

  async function download() {
    const params = currentParams()
    if ('error' in params) {
      setError(params.error as string)
      return
    }
    setError(null)
    try {
      await downloadReport(reportKey, params, exportFormat)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : `Failed to download the ${exportFormat.toUpperCase()}`)
    }
  }

  // US-RPT-12's background path: submit, poll every second, auto-download on completion.
  async function exportInBackground() {
    const params = currentParams()
    if ('error' in params) {
      setError(params.error as string)
      return
    }
    setError(null)
    try {
      let job = await submitExportJob(reportKey, params, exportFormat)
      setExportJob(job)
      while (job.status === 'RUNNING') {
        await new Promise((resolve) => setTimeout(resolve, 1000))
        job = await fetchExportJob(job.id)
        setExportJob(job)
      }
      if (job.status === 'COMPLETED') {
        await downloadExportJob(job)
      } else {
        setError(job.error ?? 'Background export failed')
      }
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Background export failed')
    } finally {
      setTimeout(() => setExportJob(null), 3000)
    }
  }

  const selectedDef = REPORTS.find((r) => r.key === reportKey)

  return (
    <Box>
      <PageHeader title="Reports" />

      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Stack spacing={2}>
          <FormControl sx={{ maxWidth: 420 }}>
            <InputLabel id="report-label">Report</InputLabel>
            <Select
              labelId="report-label"
              label="Report"
              value={reportKey}
              onChange={(e) => {
                setReportKey(e.target.value as ReportKey)
                setReport(null)
                setError(null)
              }}
            >
              {visibleReports.map((r) => (
                <MenuItem key={r.key} value={r.key}>
                  {r.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Typography variant="body2" color="text.secondary">
            {selectedDef?.description}
          </Typography>

          <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap', alignItems: 'center' }}>
            {reportKey === 'asset-register' && (
              <FormControl sx={{ minWidth: 260 }}>
                <InputLabel id="scope-label">Scope (optional)</InputLabel>
                <Select
                  labelId="scope-label"
                  label="Scope (optional)"
                  value={orgNodeId}
                  onChange={(e) => setOrgNodeId(e.target.value)}
                >
                  <MenuItem value="">Entire register (your scope)</MenuItem>
                  {(orgNodesQuery.data ?? []).map((n) => (
                    <MenuItem key={n.id} value={n.id}>
                      {n.levelName}: {n.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            {reportKey === 'employee-assets' && (
              <FormControl sx={{ minWidth: 260 }} required>
                <InputLabel id="person-label">Person</InputLabel>
                <Select
                  labelId="person-label"
                  label="Person"
                  value={personId}
                  onChange={(e) => setPersonId(e.target.value)}
                >
                  {(personsQuery.data ?? []).map((p) => (
                    <MenuItem key={p.id} value={p.id}>
                      {p.fullName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            {reportKey === 'expiry' && (
              <TextField
                label="Lookahead (days)"
                type="number"
                value={withinDays}
                onChange={(e) => setWithinDays(e.target.value)}
                sx={{ width: 180 }}
              />
            )}

            {DATE_RANGE_REPORTS.includes(reportKey) && (
              <>
                <TextField label="From" type="date" value={from} onChange={(e) => setFrom(e.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }} />
                <TextField label="To" type="date" value={to} onChange={(e) => setTo(e.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }} />
              </>
            )}

            {reportKey === 'depreciation' && (
              <TextField label="As of" type="date" value={asOf} onChange={(e) => setAsOf(e.target.value)}
                slotProps={{ inputLabel: { shrink: true } }} />
            )}

            <Button variant="contained" startIcon={<PlayArrowIcon />} onClick={run} disabled={running}>
              Run report
            </Button>
            <FormControl sx={{ minWidth: 110 }} size="small">
              <InputLabel id="export-format-label">Format</InputLabel>
              <Select labelId="export-format-label" label="Format" value={exportFormat}
                onChange={(e) => setExportFormat(e.target.value as ExportFormat)}>
                <MenuItem value="xlsx">Excel</MenuItem>
                <MenuItem value="pdf">PDF</MenuItem>
                <MenuItem value="csv">CSV</MenuItem>
              </Select>
            </FormControl>
            <Button variant="outlined" startIcon={<DownloadIcon />} onClick={download}>
              Download
            </Button>
            <Button variant="outlined" onClick={exportInBackground} disabled={exportJob?.status === 'RUNNING'}>
              Export in background
            </Button>
          </Stack>
          {exportJob && (
            <Stack spacing={0.5} sx={{ maxWidth: 420 }}>
              <Typography variant="caption" color="text.secondary">
                {exportJob.status === 'RUNNING'
                  ? `Exporting ${exportJob.reportKey} as ${exportJob.format.toUpperCase()}… ${exportJob.progress}%`
                  : exportJob.status === 'COMPLETED'
                    ? 'Export complete - downloaded.'
                    : 'Export failed.'}
              </Typography>
              <LinearProgress variant="determinate" value={exportJob.progress}
                color={exportJob.status === 'FAILED' ? 'error' : 'primary'} />
            </Stack>
          )}
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {running && <LoadingSkeleton rows={5} />}

      {report && !running && (
        <Paper variant="outlined">
          <Box sx={{ p: 2, pb: 0 }}>
            <Typography variant="h6">{report.title}</Typography>
            <Typography variant="body2" color="text.secondary">
              Generated {new Date(report.generatedAt).toLocaleString()} · {report.rows.length} row
              {report.rows.length === 1 ? '' : 's'}
            </Typography>
          </Box>
          {report.rows.length === 0 ? (
            <Typography color="text.secondary" sx={{ p: 2 }}>
              No matching records - the report ran successfully and found nothing in scope.
            </Typography>
          ) : (
            <TableContainer sx={{ maxHeight: 560 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    {report.columns.map((c) => (
                      <TableCell key={c} sx={{ fontWeight: 600 }}>
                        {c}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {report.rows.map((row, i) => (
                    <TableRow key={i} hover>
                      {row.map((cell, j) => (
                        <TableCell key={j}>{cell}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Paper>
      )}
    </Box>
  )
}
