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
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../../api/errors'
import { useAccessibilityAuditQuery, useRecordAccessibilityAuditMutation } from '../hooks/useAccessibilityAuditQuery'
import type { AccessibilityAuditOutcome } from '../types'

const OUTCOME_COLOR: Record<AccessibilityAuditOutcome, 'success' | 'warning' | 'error'> = {
  PASS: 'success',
  PASS_WITH_EXCEPTIONS: 'warning',
  FAIL: 'error',
}

/** US-CMP-04: date/outcome of the latest WCAG 2.1 AA audit. */
export function AccessibilityAuditPanel({ canWrite }: { canWrite: boolean }) {
  const auditQuery = useAccessibilityAuditQuery()
  const record = useRecordAccessibilityAuditMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [auditDate, setAuditDate] = useState('')
  const [outcome, setOutcome] = useState<AccessibilityAuditOutcome>('PASS')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setAuditDate('')
    setOutcome('PASS')
    setNotes('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleSave() {
    setError(null)
    try {
      await record.mutateAsync({ auditDate, outcome, notes: notes || undefined })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to record accessibility audit')
    }
  }

  if (auditQuery.isLoading) return <LoadingSkeleton rows={2} />
  if (auditQuery.isError) return <ErrorPanel error={auditQuery.error} onRetry={() => auditQuery.refetch()} />
  const audit = auditQuery.data!

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="subtitle1">Accessibility Audit Status</Typography>
        {canWrite && (
          <Button size="small" variant="contained" onClick={openDialog}>
            Record Audit
          </Button>
        )}
      </Stack>

      {audit.outcome ? (
        <Stack spacing={0.5}>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Chip label={audit.outcome.replace(/_/g, ' ')} color={OUTCOME_COLOR[audit.outcome]} />
            <Typography variant="body2" color="text.secondary">
              {audit.auditDate}
            </Typography>
          </Stack>
          {audit.notes && (
            <Typography variant="body2" color="text.secondary">
              {audit.notes}
            </Typography>
          )}
        </Stack>
      ) : (
        <Typography variant="body2" color="text.secondary">
          {audit.notes}
        </Typography>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Accessibility Audit</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Audit Date"
              type="date"
              required
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              value={auditDate}
              onChange={(e) => setAuditDate(e.target.value)}
            />
            <FormControl fullWidth required>
              <InputLabel id="outcome-label">Outcome</InputLabel>
              <Select
                labelId="outcome-label"
                label="Outcome"
                value={outcome}
                onChange={(e) => setOutcome(e.target.value as AccessibilityAuditOutcome)}
              >
                <MenuItem value="PASS">Pass</MenuItem>
                <MenuItem value="PASS_WITH_EXCEPTIONS">Pass with Exceptions</MenuItem>
                <MenuItem value="FAIL">Fail</MenuItem>
              </Select>
            </FormControl>
            <TextField label="Notes" fullWidth multiline minRows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={!auditDate || record.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
