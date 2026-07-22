import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { EmptyState } from '../../components/common/EmptyState'
import { isApiProblem } from '../../api/errors'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import {
  downloadImportTemplate,
  EXECUTABLE_ENTITY_TYPES,
  type ImportEntityType,
  type ImportRun,
} from '../../api/migration/importApi'
import { useCommitMutation, useDryRunMutation, useImportHistoryQuery } from './hooks/useImportQuery'

const ALL_ENTITY_TYPES: ImportEntityType[] = ['ASSET', 'PERSON', 'VENDOR', 'INVENTORY_ITEM']

function label(entityType: ImportEntityType): string {
  return entityType.charAt(0) + entityType.slice(1).toLowerCase().replace(/_/g, ' ')
}

/**
 * EPIC-MIG (US-MIG-01/03/04): download a template, dry-run validate an uploaded
 * file into a per-row error report, then commit only the valid rows with a
 * reconciliation. Gated on imports:write; the history section additionally needs
 * imports:read (an Inventory Manager who runs imports is deliberately refused it).
 */
export function ImportDataPage() {
  const user = useAuthStore((s) => s.user)
  const canReadHistory = hasPermission(user, 'imports:read')

  const [entityType, setEntityType] = useState<ImportEntityType>('ASSET')
  const [file, setFile] = useState<File | null>(null)
  const [dryRun, setDryRun] = useState<ImportRun | null>(null)
  const [commitResult, setCommitResult] = useState<ImportRun | null>(null)
  const [idempotencyKey, setIdempotencyKey] = useState<string>('')
  const [error, setError] = useState<string | null>(null)

  const dryRunMutation = useDryRunMutation()
  const commitMutation = useCommitMutation()
  const historyQuery = useImportHistoryQuery(canReadHistory)

  function resetResults() {
    setDryRun(null)
    setCommitResult(null)
    setError(null)
  }

  async function handleTemplate() {
    setError(null)
    try {
      await downloadImportTemplate(entityType)
    } catch {
      setError('Failed to download the template')
    }
  }

  function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    setFile(e.target.files?.[0] ?? null)
    resetResults()
  }

  async function handleDryRun() {
    if (!file) return
    resetResults()
    try {
      const result = await dryRunMutation.mutateAsync({ entityType, file })
      setDryRun(result)
      // A fresh key per dry-run makes the commit idempotent: a retry of THIS
      // commit reuses it (no duplicates), a new file/dry-run gets a new one.
      setIdempotencyKey(crypto.randomUUID())
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Dry-run failed')
    }
  }

  async function handleCommit() {
    if (!dryRun) return
    setError(null)
    try {
      const result = await commitMutation.mutateAsync({ runId: dryRun.id, idempotencyKey })
      setCommitResult(result)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Commit failed')
    }
  }

  const committed = commitResult?.status === 'COMMITTED'

  return (
    <Box>
      <PageHeader title="Import Data" />

      <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle1" sx={{ mb: 1 }}>
          1. Choose what to import and download its template
        </Typography>
        <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            select
            label="Entity type"
            size="small"
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value as ImportEntityType)
              setFile(null)
              resetResults()
            }}
            sx={{ minWidth: 200 }}
          >
            {ALL_ENTITY_TYPES.map((t) => {
              const enabled = EXECUTABLE_ENTITY_TYPES.includes(t)
              return (
                <MenuItem key={t} value={t} disabled={!enabled}>
                  {label(t)}
                  {!enabled ? ' (coming soon)' : ''}
                </MenuItem>
              )
            })}
          </TextField>
          <Button variant="outlined" onClick={handleTemplate}>
            Download template
          </Button>
        </Stack>

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle1" sx={{ mb: 1 }}>
          2. Upload your filled-in file and validate it
        </Typography>
        <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', alignItems: 'center' }}>
          <Button variant="outlined" component="label">
            {file ? 'Change file' : 'Choose CSV file'}
            <input type="file" accept=".csv,text/csv" hidden onChange={handleFile} />
          </Button>
          {file && <Typography variant="body2">{file.name}</Typography>}
          <Button variant="contained" onClick={handleDryRun} disabled={!file || dryRunMutation.isPending}>
            {dryRunMutation.isPending ? 'Validating…' : 'Validate (dry-run)'}
          </Button>
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {dryRun && (
        <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
          <Typography variant="subtitle1" sx={{ mb: 1 }}>
            3. Review validation results
          </Typography>
          <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
            <Chip label={`${dryRun.totalRows} total`} />
            <Chip color="success" label={`${dryRun.validRows} valid`} />
            <Chip color={dryRun.invalidRows > 0 ? 'error' : 'default'} label={`${dryRun.invalidRows} invalid`} />
          </Stack>

          {dryRun.errorReport.length > 0 && (
            <Box sx={{ overflowX: 'auto', mb: 2 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Row</TableCell>
                    <TableCell>Field</TableCell>
                    <TableCell>Problem</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dryRun.errorReport.map((e, i) => (
                    <TableRow key={`${e.rowNumber}-${e.field}-${i}`}>
                      <TableCell>{e.rowNumber}</TableCell>
                      <TableCell>{e.field}</TableCell>
                      <TableCell>{e.message}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          )}

          {!committed && (
            <Button
              variant="contained"
              color="warning"
              onClick={handleCommit}
              disabled={dryRun.validRows === 0 || commitMutation.isPending}
            >
              {commitMutation.isPending
                ? 'Committing…'
                : `Commit ${dryRun.validRows} valid row${dryRun.validRows === 1 ? '' : 's'}`}
            </Button>
          )}
          {dryRun.validRows === 0 && !committed && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              Nothing to commit — fix the rows above and re-validate.
            </Typography>
          )}
        </Paper>
      )}

      {commitResult && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Import committed: {commitResult.outcome} (created / failed / skipped).
        </Alert>
      )}

      {canReadHistory && (
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle1" sx={{ mb: 1 }}>
            Import history
          </Typography>
          {historyQuery.isLoading && <LoadingSkeleton rows={3} />}
          {historyQuery.isError && <ErrorPanel error={historyQuery.error} onRetry={() => historyQuery.refetch()} />}
          {historyQuery.isSuccess && historyQuery.data.length === 0 && <EmptyState title="No imports have been run yet" />}
          {historyQuery.isSuccess && historyQuery.data.length > 0 && (
            <Box sx={{ overflowX: 'auto' }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>When</TableCell>
                    <TableCell>Entity</TableCell>
                    <TableCell>File</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Total</TableCell>
                    <TableCell>Outcome</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {historyQuery.data.map((run) => (
                    <TableRow key={run.id}>
                      <TableCell>{new Date(run.submittedAt).toLocaleString()}</TableCell>
                      <TableCell>{label(run.entityType)}</TableCell>
                      <TableCell>{run.originalFilename ?? '—'}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          color={run.status === 'COMMITTED' ? 'success' : 'default'}
                          label={run.status}
                        />
                      </TableCell>
                      <TableCell align="right">{run.totalRows}</TableCell>
                      <TableCell>{run.outcome ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          )}
        </Paper>
      )}
    </Box>
  )
}
