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
import { useOrgNodesQuery } from '../../audits/hooks/useOrgNodesQuery'
import {
  useApproveTransferMutation,
  useCreateTransferMutation,
  useRejectTransferMutation,
  useTransfersQuery,
} from '../../lifecycle/hooks/useTransfersQuery'
import type { Asset } from '../types'
import type { LifecycleRequestStatus } from '../../lifecycle/types'

const STATUS_COLOR: Record<LifecycleRequestStatus, 'warning' | 'success' | 'error'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

/** US-LIF-05/10/11: request an asset transfer between org nodes/custodians, with approval. */
export function AssetTransferPanel({ asset }: { asset: Asset }) {
  const transfersQuery = useTransfersQuery(asset.id)
  const createTransfer = useCreateTransferMutation(asset.id)
  const approveTransfer = useApproveTransferMutation(asset.id)
  const rejectTransfer = useRejectTransferMutation(asset.id)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')
  const canApprove = hasPermission(useAuthStore((s) => s.user), 'approvals:write')

  const [dialogOpen, setDialogOpen] = useState(false)
  const orgNodesQuery = useOrgNodesQuery()
  const usersQuery = usePickableUsersQuery(dialogOpen)
  const [toOrgNodeId, setToOrgNodeId] = useState('')
  const [reason, setReason] = useState('')
  const [nominalApproverId, setNominalApproverId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [decisionError, setDecisionError] = useState<string | null>(null)
  const [rejectTarget, setRejectTarget] = useState<string | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  function openDialog() {
    setToOrgNodeId('')
    setReason('')
    setNominalApproverId('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await createTransfer.mutateAsync({ toOrgNodeId, reason, nominalApproverId })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to request transfer')
    }
  }

  // US-USR-06 (AC-USR-06-X): a self-approval SoD block comes back as a 403 with an
  // actionable message ("route it to another approver, or record a waiver") - surfaced
  // here rather than failing silently, so the approver understands why it was refused.
  async function handleApprove(id: string) {
    setDecisionError(null)
    try {
      await approveTransfer.mutateAsync(id)
    } catch (err) {
      setDecisionError(isApiProblem(err) ? err.detail : 'Failed to approve this transfer')
    }
  }

  async function handleReject() {
    if (!rejectTarget) return
    setDecisionError(null)
    try {
      await rejectTransfer.mutateAsync({ id: rejectTarget, reason: rejectReason })
      setRejectTarget(null)
      setRejectReason('')
    } catch (err) {
      setRejectTarget(null)
      setDecisionError(isApiProblem(err) ? err.detail : 'Failed to reject this transfer')
    }
  }

  return (
    <Box>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Typography variant="subtitle1">Transfers</Typography>
        {canWrite && asset.status.code !== 'DISPOSED' && (
          <Button size="small" onClick={openDialog}>
            Request Transfer
          </Button>
        )}
      </Stack>

      {decisionError && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setDecisionError(null)}>
          {decisionError}
        </Alert>
      )}

      <List dense>
        {(transfersQuery.data ?? []).map((transfer) => (
          <ListItem
            key={transfer.id}
            divider
            secondaryAction={
              canApprove &&
              transfer.status === 'PENDING' && (
                <Stack direction="row" spacing={0.5}>
                  <Button size="small" color="success" onClick={() => handleApprove(transfer.id)}>
                    Approve
                  </Button>
                  <Button size="small" color="error" onClick={() => setRejectTarget(transfer.id)}>
                    Reject
                  </Button>
                </Stack>
              )
            }
          >
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Chip size="small" color={STATUS_COLOR[transfer.status]} label={transfer.status} />
                  <Typography variant="body2">
                    {transfer.fromOrgNodeCode ?? '—'} → {transfer.toOrgNodeCode}
                  </Typography>
                </Stack>
              }
              secondary={transfer.rejectionReason ? `${transfer.reason} — rejected: ${transfer.rejectionReason}` : transfer.reason}
            />
          </ListItem>
        ))}
        {(transfersQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No transfer requests for this asset.
          </Typography>
        )}
      </List>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Transfer</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth size="small">
              <InputLabel id="to-org-node-label">Destination Location</InputLabel>
              <Select
                labelId="to-org-node-label"
                label="Destination Location"
                value={toOrgNodeId}
                onChange={(e) => setToOrgNodeId(e.target.value)}
              >
                {(orgNodesQuery.data ?? [])
                  .filter((n) => n.id !== asset.orgNodeId)
                  .map((node) => (
                    <MenuItem key={node.id} value={node.id}>
                      {node.name} ({node.levelName})
                    </MenuItem>
                  ))}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel id="transfer-approver-label">Approver</InputLabel>
              <Select
                labelId="transfer-approver-label"
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
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={!toOrgNodeId || !reason || !nominalApproverId || createTransfer.isPending}
          >
            Submit Request
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!rejectTarget} onClose={() => setRejectTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Transfer</DialogTitle>
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
          <Button color="error" variant="contained" onClick={handleReject} disabled={!rejectReason || rejectTransfer.isPending}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
