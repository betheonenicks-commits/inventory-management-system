import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
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
import { isApiProblem } from '../../../api/errors'
import { useAuthStore, hasPermission } from '../../../auth/authStore'
import { usePickableUsersQuery } from '../../users/hooks/useUsersQuery'
import {
  useApproveDisposalMutation,
  useCreateDisposalMutation,
  useDisposalsQuery,
  useRejectDisposalMutation,
  useRestoreDisposalMutation,
} from '../../lifecycle/hooks/useDisposalsQuery'
import type { Asset } from '../types'
import type { DisposalType, LifecycleRequestStatus } from '../../lifecycle/types'

const STATUS_COLOR: Record<LifecycleRequestStatus, 'warning' | 'success' | 'error'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

/** US-LIF-09/10/11/12: retire/dispose/donate with approval, restorable within a configurable window. */
export function AssetDisposalPanel({ asset }: { asset: Asset }) {
  const disposalsQuery = useDisposalsQuery(asset.id)
  const createDisposal = useCreateDisposalMutation(asset.id)
  const approveDisposal = useApproveDisposalMutation(asset.id)
  const rejectDisposal = useRejectDisposalMutation(asset.id)
  const restoreDisposal = useRestoreDisposalMutation(asset.id)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')
  const canApprove = hasPermission(useAuthStore((s) => s.user), 'approvals:write')
  const canRestore = hasPermission(useAuthStore((s) => s.user), 'assets:restore')

  const [dialogOpen, setDialogOpen] = useState(false)
  const usersQuery = usePickableUsersQuery(dialogOpen)
  const [disposalType, setDisposalType] = useState<DisposalType>('RETIRE')
  const [reason, setReason] = useState('')
  const [nominalApproverId, setNominalApproverId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [rejectTarget, setRejectTarget] = useState<string | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  function openDialog() {
    setDisposalType('RETIRE')
    setReason('')
    setNominalApproverId('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await createDisposal.mutateAsync({ disposalType, reason, nominalApproverId })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to request disposal')
    }
  }

  async function handleReject() {
    if (!rejectTarget) return
    try {
      await rejectDisposal.mutateAsync({ id: rejectTarget, reason: rejectReason })
      setRejectTarget(null)
      setRejectReason('')
    } catch {
      // errors surface via the list state on refetch
    }
  }

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Typography variant="subtitle1">Disposal</Typography>
        {canWrite && asset.status.code !== 'DISPOSED' && (
          <Button size="small" onClick={openDialog}>
            Request Disposal
          </Button>
        )}
      </Stack>

      <List dense>
        {(disposalsQuery.data ?? []).map((disposal) => (
          <ListItem
            key={disposal.id}
            divider
            secondaryAction={
              <Stack direction="row" spacing={0.5}>
                {canApprove && disposal.status === 'PENDING' && (
                  <>
                    <Button size="small" color="success" onClick={() => approveDisposal.mutate(disposal.id)}>
                      Approve
                    </Button>
                    <Button size="small" color="error" onClick={() => setRejectTarget(disposal.id)}>
                      Reject
                    </Button>
                  </>
                )}
                {canRestore && disposal.status === 'APPROVED' && !disposal.restoredAt && (
                  <Button size="small" onClick={() => restoreDisposal.mutate(disposal.id)}>
                    Restore
                  </Button>
                )}
              </Stack>
            }
          >
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Chip size="small" color={STATUS_COLOR[disposal.status]} label={disposal.status} />
                  <Typography variant="body2">{disposal.disposalType}</Typography>
                  {disposal.restoredAt && <Chip size="small" label="RESTORED" />}
                </Stack>
              }
              secondary={disposal.rejectionReason ? `${disposal.reason} — rejected: ${disposal.rejectionReason}` : disposal.reason}
            />
          </ListItem>
        ))}
        {(disposalsQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No disposal requests for this asset.
          </Typography>
        )}
      </List>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Disposal</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth size="small">
              <InputLabel id="disposal-type-label">Type</InputLabel>
              <Select
                labelId="disposal-type-label"
                label="Type"
                value={disposalType}
                onChange={(e) => setDisposalType(e.target.value as DisposalType)}
              >
                <MenuItem value="RETIRE">Retire</MenuItem>
                <MenuItem value="DISPOSE">Dispose</MenuItem>
                <MenuItem value="DONATE">Donate</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel id="disposal-approver-label">Approver</InputLabel>
              <Select
                labelId="disposal-approver-label"
                label="Approver"
                value={nominalApproverId}
                onChange={(e) => setNominalApproverId(e.target.value)}
              >
                {(usersQuery.data ?? []).map((user) => (
                  <MenuItem key={user.id} value={user.id}>
                    {user.displayName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Reason"
              required
              fullWidth
              multiline
              minRows={2}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!reason || !nominalApproverId || createDisposal.isPending}>
            Submit Request
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!rejectTarget} onClose={() => setRejectTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Disposal</DialogTitle>
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
          <Button onClick={() => setRejectTarget(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleReject} disabled={!rejectReason || rejectDisposal.isPending}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
