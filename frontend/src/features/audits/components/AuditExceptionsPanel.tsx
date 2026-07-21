import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import PhotoCameraIcon from '@mui/icons-material/PhotoCamera'
import { isApiProblem } from '../../../api/errors'
import { useAuthStore, hasPermission } from '../../../auth/authStore'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { fetchFindingEvidenceBlobUrl } from '../../../api/audits/auditApi'
import {
  useAuditExceptionsQuery,
  useCorrectFindingMutation,
  useFindingEvidenceQuery,
  useReconcileFindingMutation,
  useResolveScopeChangeMutation,
  useUploadEvidenceMutation,
} from '../hooks/useAuditsQuery'
import type { AuditFinding, CorrectionField, FindingStatus, ScopeChangeDisposition } from '../types'

function describeError(err: unknown, fallback: string): string {
  if (!isApiProblem(err)) return fallback
  if (err.errors && err.errors.length > 0) return err.errors.map((e) => e.message).join(' ')
  return err.detail
}

const STATUS_COLOR: Record<FindingStatus, 'success' | 'error' | 'warning'> = {
  VERIFIED: 'success',
  MISSING: 'error',
  OUT_OF_SCOPE: 'warning',
  SCOPE_CHANGED: 'warning',
}

/**
 * US-AUD-21: the button that opens the reconcile dialog - a small control, which is
 * all MUI's ListItem `secondaryAction` slot is designed to hold. The reconciliation
 * *outcome* (a full sentence) deliberately renders in the row's `secondary` text
 * instead, alongside remarks - not here, where a long string would overlap the
 * primary content rather than wrap within it.
 */
function ReconcileAction({ auditId, finding, canWrite }: { auditId: string; finding: AuditFinding; canWrite: boolean }) {
  const reconcileFinding = useReconcileFindingMutation(auditId)
  const [open, setOpen] = useState(false)
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (finding.status !== 'MISSING' || finding.reconciliation || !canWrite) return null

  async function handleReconcile() {
    setError(null)
    try {
      await reconcileFinding.mutateAsync({ findingId: finding.id, foundLocationNote: note })
      setOpen(false)
      setNote('')
    } catch (err) {
      setError(describeError(err, 'Failed to reconcile finding'))
    }
  }

  return (
    <>
      <Button size="small" onClick={() => setOpen(true)}>
        Reconcile
      </Button>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Reconcile {finding.assetNumber} — {finding.assetName}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              This asset was found after being classified Missing. The original finding stays on record unchanged; this
              records a new, linked outcome and reverts the asset's status.
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Where / how was it found"
              required
              fullWidth
              multiline
              minRows={2}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleReconcile} disabled={!note.trim() || reconcileFinding.isPending}>
            Reconcile
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

const CONDITIONS = ['GOOD', 'FAIR', 'MINOR_DAMAGE', 'MAJOR_DAMAGE', 'UNUSABLE'] as const

const DISPOSITION_LABEL: Record<ScopeChangeDisposition, string> = {
  CONFIRM_VERIFIED_AT_NEW_LOCATION: 'Confirmed verified at new location',
  EXCLUDE_FROM_SCOPE: 'Excluded from scope',
  ACCEPT_AS_EXCEPTION: 'Accepted as exception',
}

/**
 * US-AUD-23: resolve a Scope Changed finding so it stops permanently blocking closure
 * (AuditWorkflowService.submit's AC-AUD-23-X gate) - previously nothing in the product
 * could ever set this. Settable exactly once; once resolved, the row shows the outcome
 * instead of the button (mirrors ReconcileAction's shape for a Missing finding).
 */
function ResolveScopeChangeAction({ auditId, finding, canWrite }: { auditId: string; finding: AuditFinding; canWrite: boolean }) {
  const resolveScopeChange = useResolveScopeChangeMutation(auditId)
  const [open, setOpen] = useState(false)
  const [disposition, setDisposition] = useState<ScopeChangeDisposition>('CONFIRM_VERIFIED_AT_NEW_LOCATION')
  const [error, setError] = useState<string | null>(null)

  if (finding.status !== 'SCOPE_CHANGED' || finding.scopeChangeDisposition || !canWrite) return null

  async function handleResolve() {
    setError(null)
    try {
      await resolveScopeChange.mutateAsync({ findingId: finding.id, disposition })
      setOpen(false)
    } catch (err) {
      setError(describeError(err, 'Failed to resolve scope change'))
    }
  }

  return (
    <>
      <Button size="small" onClick={() => setOpen(true)}>
        Resolve
      </Button>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Resolve scope change — {finding.assetNumber} — {finding.assetName}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              This asset moved (transfer or disposal) while this audit was in progress, before it was scanned. Choose
              how to resolve it - this audit cannot be submitted while any Scope Changed finding is unresolved.
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            <FormControl fullWidth size="small">
              <InputLabel id="disposition-label">Disposition</InputLabel>
              <Select
                labelId="disposition-label"
                label="Disposition"
                value={disposition}
                onChange={(e) => setDisposition(e.target.value as ScopeChangeDisposition)}
              >
                {(Object.keys(DISPOSITION_LABEL) as ScopeChangeDisposition[]).map((d) => (
                  <MenuItem key={d} value={d}>
                    {DISPOSITION_LABEL[d]}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleResolve} disabled={resolveScopeChange.isPending}>
            Resolve
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

/**
 * US-AUD-24: correct a finding's condition or remarks. This is never an edit to the
 * original finding (there is no PATCH/DELETE for findings anywhere - that absence is
 * what actually enforces AC-AUD-24-X) - it always writes a new, linked, immutable
 * correction record, and the row's "current" condition/remarks shown elsewhere already
 * reflect the latest correction via the backend's `effectiveValue` resolution.
 */
function CorrectFindingAction({ auditId, finding, canWrite }: { auditId: string; finding: AuditFinding; canWrite: boolean }) {
  const correctFinding = useCorrectFindingMutation(auditId)
  const [open, setOpen] = useState(false)
  const [fieldName, setFieldName] = useState<CorrectionField>('CONDITION')
  const [newValue, setNewValue] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (!canWrite) return null

  function handleOpen() {
    setFieldName('CONDITION')
    setNewValue('')
    setError(null)
    setOpen(true)
  }

  async function handleCorrect() {
    if (!newValue.trim()) return
    setError(null)
    try {
      await correctFinding.mutateAsync({ findingId: finding.id, fieldName, newValue })
      setOpen(false)
    } catch (err) {
      setError(describeError(err, 'Failed to correct finding'))
    }
  }

  return (
    <>
      <Button size="small" onClick={handleOpen}>
        Correct
      </Button>
      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Correct {finding.assetNumber} — {finding.assetName}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              This records a new correction linked to the finding - the original scan is never edited or deleted.
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            <FormControl fullWidth size="small">
              <InputLabel id="correction-field-label">Field</InputLabel>
              <Select
                labelId="correction-field-label"
                label="Field"
                value={fieldName}
                onChange={(e) => {
                  setFieldName(e.target.value as CorrectionField)
                  setNewValue('')
                }}
              >
                <MenuItem value="CONDITION">Condition</MenuItem>
                <MenuItem value="REMARKS">Remarks</MenuItem>
              </Select>
            </FormControl>
            {fieldName === 'CONDITION' ? (
              <FormControl fullWidth size="small">
                <InputLabel id="correction-value-label">New condition</InputLabel>
                <Select
                  labelId="correction-value-label"
                  label="New condition"
                  value={newValue}
                  onChange={(e) => setNewValue(e.target.value)}
                >
                  {CONDITIONS.map((c) => (
                    <MenuItem key={c} value={c}>
                      {c.replace('_', ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            ) : (
              <TextField
                label="New remarks"
                fullWidth
                multiline
                minRows={2}
                value={newValue}
                onChange={(e) => setNewValue(e.target.value)}
              />
            )}
            {finding.corrections.length > 0 && (
              <Stack spacing={0.5}>
                <Typography variant="caption" color="text.secondary">
                  Correction history
                </Typography>
                {finding.corrections.map((c) => (
                  <Typography key={c.id} variant="caption" color="text.secondary">
                    {c.fieldName}: {c.oldValue ?? '(none)'} → {c.newValue} by {c.actorUsername}
                  </Typography>
                ))}
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCorrect} disabled={!newValue.trim() || correctFinding.isPending}>
            Save Correction
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
}

/**
 * US-AUD-11: photo evidence on a finding. The photo chips fetch bytes through
 * the backend's brokered download (with the auth header) into an object URL -
 * there is deliberately no direct object-store URL anywhere in the client.
 */
function FindingEvidence({ auditId, finding, canWrite }: { auditId: string; finding: AuditFinding; canWrite: boolean }) {
  const evidenceQuery = useFindingEvidenceQuery(auditId, finding.id)
  const uploadEvidence = useUploadEvidenceMutation(auditId)
  const [error, setError] = useState<string | null>(null)

  async function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setError(null)
    try {
      await uploadEvidence.mutateAsync({ findingId: finding.id, file })
    } catch (err) {
      setError(describeError(err, 'Failed to upload evidence'))
    }
  }

  async function handleView(attachmentId: string) {
    try {
      const url = await fetchFindingEvidenceBlobUrl(auditId, finding.id, attachmentId)
      window.open(url, '_blank', 'noopener')
      // The new tab holds its own reference; revoke ours once it has had time to load.
      setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (err) {
      setError(describeError(err, 'Failed to open evidence'))
    }
  }

  const evidence = evidenceQuery.data ?? []
  if (!canWrite && evidence.length === 0) return null

  return (
    <Stack spacing={0.5} sx={{ mt: 0.5 }}>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        {evidence.map((item) => (
          <Chip
            key={item.id}
            size="small"
            variant="outlined"
            icon={<PhotoCameraIcon />}
            label={item.fileName}
            onClick={() => handleView(item.id)}
          />
        ))}
        {canWrite && (
          <Button size="small" component="label" disabled={uploadEvidence.isPending}>
            {uploadEvidence.isPending ? 'Uploading…' : 'Attach photo'}
            <input hidden type="file" accept="image/jpeg,image/png,image/webp" onChange={handleFile} />
          </Button>
        )}
      </Stack>
      {error && (
        <Alert severity="error" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
    </Stack>
  )
}

/** US-AUD-16: everything that wasn't clean - Missing, Damaged, Out of Scope, Scope Changed. */
export function AuditExceptionsPanel({ auditId }: { auditId: string }) {
  const exceptionsQuery = useAuditExceptionsQuery(auditId)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'audits:write')

  if (exceptionsQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (exceptionsQuery.isError) return <ErrorPanel error={exceptionsQuery.error} onRetry={() => exceptionsQuery.refetch()} />
  const report = exceptionsQuery.data!

  return (
    <Stack spacing={1}>
      <Typography variant="subtitle1">Exceptions</Typography>
      {!report.hasExceptions ? (
        <Typography variant="body2" color="text.secondary">
          No exceptions - every scanned asset in scope came back clean.
        </Typography>
      ) : (
        <List dense>
          {report.findings.map((finding) => (
            <ListItem
              key={finding.id}
              divider
              secondaryAction={
                <Stack direction="row" spacing={0.5}>
                  <CorrectFindingAction auditId={auditId} finding={finding} canWrite={canWrite} />
                  <ReconcileAction auditId={auditId} finding={finding} canWrite={canWrite} />
                  <ResolveScopeChangeAction auditId={auditId} finding={finding} canWrite={canWrite} />
                </Stack>
              }
            >
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Typography variant="body2">
                      {finding.assetNumber} — {finding.assetName}
                    </Typography>
                    <Chip size="small" label={finding.status.replace('_', ' ')} color={STATUS_COLOR[finding.status]} />
                    {finding.condition && <Chip size="small" variant="outlined" label={finding.condition.replace('_', ' ')} />}
                    {finding.corrections.length > 0 && (
                      <Chip size="small" variant="outlined" color="info" label={`Corrected ×${finding.corrections.length}`} />
                    )}
                  </Stack>
                }
                slotProps={{ secondary: { component: 'div' } }}
                secondary={
                  <Stack spacing={0.25}>
                    {finding.remarks && <Typography variant="body2">{finding.remarks}</Typography>}
                    {finding.reconciliation && (
                      <Typography variant="caption" color="success.main">
                        Reconciled by {finding.reconciliation.reconciledByUsername}: {finding.reconciliation.foundLocationNote}
                      </Typography>
                    )}
                    {finding.scopeChangeDisposition && (
                      <Typography variant="caption" color="success.main">
                        Resolved: {DISPOSITION_LABEL[finding.scopeChangeDisposition]}
                      </Typography>
                    )}
                    <FindingEvidence auditId={auditId} finding={finding} canWrite={canWrite} />
                  </Stack>
                }
              />
            </ListItem>
          ))}
        </List>
      )}
    </Stack>
  )
}
