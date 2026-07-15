import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../../api/errors'
import { useAuthStore, hasPermission } from '../../../auth/authStore'
import { useAuditCertificateQuery } from '../hooks/useAuditsQuery'
import {
  useApproveAuditMutation,
  useEscalateAuditMutation,
  useRejectAuditMutation,
  useSubmitAuditMutation,
} from '../hooks/useAuditWorkflowMutations'
import type { Audit } from '../types'

// ValidationFailedException-based errors (e.g. US-AUD-22's self-approval block)
// carry the actually-useful message in errors[], not in the generic top-level
// `detail` string ("One or more fields failed validation.") - found via
// browser-testing the self-approval path, which otherwise showed only that
// generic text with the real "you cannot submit it yourself" reason hidden.
function describeError(err: unknown, fallback: string): string {
  if (!isApiProblem(err)) return fallback
  if (err.errors && err.errors.length > 0) return err.errors.map((e) => e.message).join(' ')
  return err.detail
}

/** US-AUD-13/14/15: sign and submit, then Department Head approve/reject, then a completion certificate. */
export function AuditWorkflowPanel({ audit }: { audit: Audit }) {
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'audits:write')
  const canApprove = hasPermission(useAuthStore((s) => s.user), 'approvals:write')

  const submitAudit = useSubmitAuditMutation(audit.id)
  const approveAudit = useApproveAuditMutation(audit.id)
  const rejectAudit = useRejectAuditMutation(audit.id)
  const escalateAudit = useEscalateAuditMutation(audit.id)
  const certificateQuery = useAuditCertificateQuery(audit.id, audit.status === 'CLOSED')

  const [submitOpen, setSubmitOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [signatureName, setSignatureName] = useState('')
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [escalateNotice, setEscalateNotice] = useState<string | null>(null)

  async function handleSubmit() {
    setError(null)
    try {
      await submitAudit.mutateAsync({ password, signatureName: signatureName || undefined })
      setSubmitOpen(false)
      setPassword('')
      setSignatureName('')
    } catch (err) {
      setError(describeError(err, 'Failed to submit audit'))
    }
  }

  async function handleApprove() {
    setError(null)
    try {
      await approveAudit.mutateAsync()
    } catch (err) {
      setError(describeError(err, 'Failed to approve audit'))
    }
  }

  async function handleReject() {
    setError(null)
    try {
      await rejectAudit.mutateAsync(rejectReason)
      setRejectOpen(false)
      setRejectReason('')
    } catch (err) {
      setError(describeError(err, 'Failed to reject audit'))
    }
  }

  async function handleEscalate() {
    setError(null)
    setEscalateNotice(null)
    try {
      await escalateAudit.mutateAsync()
      setEscalateNotice('Escalated - the approval now also routes to the next resolver in line.')
    } catch (err) {
      // Most common case: ESCALATION_THRESHOLD_NOT_REACHED (409) - this audit hasn't sat long enough yet.
      setError(describeError(err, 'Failed to escalate audit'))
    }
  }

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Workflow</Typography>

      {error && <Alert severity="error">{error}</Alert>}
      {escalateNotice && <Alert severity="success">{escalateNotice}</Alert>}

      {audit.status === 'IN_PROGRESS' && canWrite && (
        <Box>
          <Button variant="contained" onClick={() => setSubmitOpen(true)}>
            Submit for Approval
          </Button>
        </Box>
      )}

      {audit.status === 'PENDING_APPROVAL' && (
        <Stack spacing={1}>
          {audit.lastRejectionReason && (
            <Alert severity="info">Previously rejected: {audit.lastRejectionReason}</Alert>
          )}
          {canApprove ? (
            <Stack direction="row" spacing={1}>
              <Button variant="contained" color="success" onClick={handleApprove} disabled={approveAudit.isPending}>
                Approve &amp; Close
              </Button>
              <Button variant="outlined" color="error" onClick={() => setRejectOpen(true)}>
                Reject
              </Button>
              <Button variant="text" onClick={handleEscalate} disabled={escalateAudit.isPending}>
                Escalate
              </Button>
            </Stack>
          ) : (
            <Typography variant="body2" color="text.secondary">
              Awaiting Department Head approval.
            </Typography>
          )}
        </Stack>
      )}

      {audit.status === 'CLOSED' && (
        <Stack spacing={1}>
          <Alert severity="success">Closed and approved.</Alert>
          {certificateQuery.data && (
            <Stack spacing={0.5}>
              <Typography variant="body2">Completion Certificate</Typography>
              <Typography variant="body2" color="text.secondary">
                Expected {certificateQuery.data.expectedCount} · Verified {certificateQuery.data.verifiedCount} · Missing{' '}
                {certificateQuery.data.missingCount} · Damaged {certificateQuery.data.damagedCount}
              </Typography>
            </Stack>
          )}
        </Stack>
      )}

      <Dialog open={submitOpen} onClose={() => setSubmitOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Submit Audit</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Re-authenticate to sign and submit this audit (US-AUD-13). Any expected assets not yet scanned will be
              classified Missing.
            </Typography>
            <TextField
              label="Your Password"
              type="password"
              required
              fullWidth
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <TextField
              label="Signature Name"
              fullWidth
              value={signatureName}
              onChange={(e) => setSignatureName(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSubmitOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit} disabled={!password || submitAudit.isPending}>
            Sign &amp; Submit
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={rejectOpen} onClose={() => setRejectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Audit</DialogTitle>
        <DialogContent>
          <TextField
            label="Reason"
            required
            fullWidth
            multiline
            minRows={2}
            sx={{ mt: 1 }}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectOpen(false)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleReject} disabled={!rejectReason || rejectAudit.isPending}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
