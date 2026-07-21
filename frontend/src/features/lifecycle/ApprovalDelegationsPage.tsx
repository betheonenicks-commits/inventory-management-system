import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../api/errors'
import { usePickableUsersQuery } from '../users/hooks/useUsersQuery'
import {
  useApprovalDelegationsQuery,
  useCreateApprovalDelegationMutation,
  useMeQuery,
  useRevokeApprovalDelegationMutation,
} from './hooks/useApprovalDelegationsQuery'

/**
 * US-LIF-15: a Department Head (or anyone routed approvals) delegating their
 * approval authority to a named alternate for a defined window - the
 * backend (create/list/revoke, and TransferService/DisposalService/
 * AuditWorkflowService all consulting the active delegation when routing)
 * was fully built with no way to reach it from the product; this is that
 * screen. Self-service only - a delegator always acts as themselves
 * (resolved via /auth/me), matching the story's own framing.
 */
export function ApprovalDelegationsPage() {
  const meQuery = useMeQuery()
  const myUserId = meQuery.data?.id
  const delegationsQuery = useApprovalDelegationsQuery(myUserId)
  const usersQuery = usePickableUsersQuery()
  const createDelegation = useCreateApprovalDelegationMutation(myUserId)
  const revokeDelegation = useRevokeApprovalDelegationMutation(myUserId)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [delegateUserId, setDelegateUserId] = useState('')
  const [validFrom, setValidFrom] = useState('')
  const [validTo, setValidTo] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setDelegateUserId('')
    setValidFrom('')
    setValidTo('')
    setReason('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await createDelegation.mutateAsync({
        delegateUserId,
        validFrom: new Date(validFrom).toISOString(),
        validTo: new Date(validTo).toISOString(),
        reason: reason || undefined,
      })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create delegation')
    }
  }

  async function handleRevoke(id: string) {
    try {
      await revokeDelegation.mutateAsync(id)
    } catch {
      // Revocation failures are rare (already-revoked, not-found) - the list
      // simply won't reflect a change; no dedicated error surface needed here.
    }
  }

  const otherUsers = (usersQuery.data ?? []).filter((u) => u.id !== myUserId)

  return (
    <Box>
      <PageHeader
        title="Approval Delegations"
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog} disabled={!myUserId}>
            New Delegation
          </Button>
        }
      />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        While active, anything routed to you for approval (transfers, disposals, audits) routes to your delegate
        instead.
      </Typography>

      {(meQuery.isLoading || delegationsQuery.isLoading) && <LoadingSkeleton rows={3} />}
      {delegationsQuery.isError && (
        <ErrorPanel error={delegationsQuery.error} onRetry={() => delegationsQuery.refetch()} />
      )}

      {delegationsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {delegationsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No delegations yet.</Typography>
              </Box>
            )}
            {delegationsQuery.data.map((d) => {
              const delegate = (usersQuery.data ?? []).find((u) => u.id === d.delegateUserId)
              const now = Date.now()
              const inWindow = d.active && new Date(d.validFrom).getTime() <= now && now <= new Date(d.validTo).getTime()
              return (
                <ListItem
                  key={d.id}
                  divider
                  secondaryAction={
                    d.active && (
                      <Button size="small" onClick={() => handleRevoke(d.id)} disabled={revokeDelegation.isPending}>
                        Revoke
                      </Button>
                    )
                  }
                >
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Typography variant="body2">To {delegate?.displayName ?? d.delegateUserId}</Typography>
                        {!d.active ? (
                          <Chip size="small" label="Revoked" />
                        ) : inWindow ? (
                          <Chip size="small" color="success" label="Active now" />
                        ) : (
                          <Chip size="small" color="default" variant="outlined" label="Scheduled" />
                        )}
                      </Stack>
                    }
                    secondary={
                      <Stack spacing={0.25}>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(d.validFrom).toLocaleString()} — {new Date(d.validTo).toLocaleString()}
                        </Typography>
                        {d.reason && <Typography variant="body2">{d.reason}</Typography>}
                      </Stack>
                    }
                  />
                </ListItem>
              )
            })}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Approval Delegation</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              select
              label="Delegate to"
              fullWidth
              required
              value={delegateUserId}
              onChange={(e) => setDelegateUserId(e.target.value)}
            >
              {otherUsers.map((u) => (
                <MenuItem key={u.id} value={u.id}>
                  {u.displayName}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Valid from"
              type="datetime-local"
              fullWidth
              required
              value={validFrom}
              onChange={(e) => setValidFrom(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label="Valid to"
              type="datetime-local"
              fullWidth
              required
              value={validTo}
              onChange={(e) => setValidTo(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label="Reason (optional)"
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
            disabled={!delegateUserId || !validFrom || !validTo || createDelegation.isPending}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
