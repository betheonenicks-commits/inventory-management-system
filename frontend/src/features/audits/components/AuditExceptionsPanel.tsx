import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../../api/errors'
import { useAuthStore, hasPermission } from '../../../auth/authStore'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { useAuditExceptionsQuery, useReconcileFindingMutation } from '../hooks/useAuditsQuery'
import type { AuditFinding, FindingStatus } from '../types'

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
              secondaryAction={<ReconcileAction auditId={auditId} finding={finding} canWrite={canWrite} />}
            >
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Typography variant="body2">
                      {finding.assetNumber} — {finding.assetName}
                    </Typography>
                    <Chip size="small" label={finding.status.replace('_', ' ')} color={STATUS_COLOR[finding.status]} />
                    {finding.condition && <Chip size="small" variant="outlined" label={finding.condition.replace('_', ' ')} />}
                  </Stack>
                }
                slotProps={{ secondary: { component: 'div' } }}
                secondary={
                  (finding.remarks || finding.reconciliation) && (
                    <Stack spacing={0.25}>
                      {finding.remarks && <Typography variant="body2">{finding.remarks}</Typography>}
                      {finding.reconciliation && (
                        <Typography variant="caption" color="success.main">
                          Reconciled by {finding.reconciliation.reconciledByUsername}: {finding.reconciliation.foundLocationNote}
                        </Typography>
                      )}
                    </Stack>
                  )
                }
              />
            </ListItem>
          ))}
        </List>
      )}
    </Stack>
  )
}
