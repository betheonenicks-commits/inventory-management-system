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
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { usePickableUsersQuery } from '../users/hooks/useUsersQuery'
import {
  useApprovePurchaseRequestMutation,
  useCreatePurchaseRequestMutation,
  usePurchaseRequestsQuery,
  useRejectPurchaseRequestMutation,
} from './hooks/usePurchaseRequestsQuery'
import type { LifecycleRequestStatus } from '../lifecycle/types'

const STATUS_TABS: Array<{ label: string; value: LifecycleRequestStatus | undefined }> = [
  { label: 'All', value: undefined },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'APPROVED' },
  { label: 'Rejected', value: 'REJECTED' },
]

const STATUS_COLOR: Record<LifecycleRequestStatus, 'warning' | 'success' | 'error'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

/** US-LIF-01: submit and approve/reject a purchase request. */
export function PurchaseRequestListPage() {
  const [statusTab, setStatusTab] = useState(0)
  const status = STATUS_TABS[statusTab].value
  const requestsQuery = usePurchaseRequestsQuery(status)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')
  const canApprove = hasPermission(useAuthStore((s) => s.user), 'approvals:write')
  const createRequest = useCreatePurchaseRequestMutation()
  const approveRequest = useApprovePurchaseRequestMutation()
  const rejectRequest = useRejectPurchaseRequestMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const usersQuery = usePickableUsersQuery(dialogOpen)
  const [itemDescription, setItemDescription] = useState('')
  const [justification, setJustification] = useState('')
  const [estimatedCost, setEstimatedCost] = useState('')
  const [vendorName, setVendorName] = useState('')
  const [nominalApproverId, setNominalApproverId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [rejectTarget, setRejectTarget] = useState<string | null>(null)
  const [rejectReason, setRejectReason] = useState('')

  function openDialog() {
    setItemDescription('')
    setJustification('')
    setEstimatedCost('')
    setVendorName('')
    setNominalApproverId('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await createRequest.mutateAsync({
        itemDescription,
        justification,
        estimatedCost: estimatedCost ? Number(estimatedCost) : undefined,
        vendorName: vendorName || undefined,
        nominalApproverId,
      })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to submit purchase request')
    }
  }

  async function handleReject() {
    if (!rejectTarget) return
    try {
      await rejectRequest.mutateAsync({ id: rejectTarget, reason: rejectReason })
      setRejectTarget(null)
      setRejectReason('')
    } catch {
      // errors surface via refetch
    }
  }

  return (
    <Box>
      <PageHeader
        title="Purchase Requests"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
              New Request
            </Button>
          )
        }
      />

      <Tabs value={statusTab} onChange={(_, v) => setStatusTab(v)} sx={{ mb: 2 }}>
        {STATUS_TABS.map((tab) => (
          <Tab key={tab.label} label={tab.label} />
        ))}
      </Tabs>

      {requestsQuery.isLoading && <LoadingSkeleton rows={6} />}
      {requestsQuery.isError && <ErrorPanel error={requestsQuery.error} onRetry={() => requestsQuery.refetch()} />}

      {requestsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {requestsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No purchase requests in this view.</Typography>
              </Box>
            )}
            {requestsQuery.data.map((request) => (
              <ListItem
                key={request.id}
                divider
                secondaryAction={
                  canApprove &&
                  request.status === 'PENDING' && (
                    <Stack direction="row" spacing={0.5}>
                      <Button size="small" color="success" onClick={() => approveRequest.mutate(request.id)}>
                        Approve
                      </Button>
                      <Button size="small" color="error" onClick={() => setRejectTarget(request.id)}>
                        Reject
                      </Button>
                    </Stack>
                  )
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{request.itemDescription}</Typography>
                      <Chip size="small" label={request.status} color={STATUS_COLOR[request.status]} />
                    </Stack>
                  }
                  slotProps={{ secondary: { component: 'div' } }}
                  secondary={
                    <Typography variant="body2" color="text.secondary">
                      {request.justification}
                      {request.estimatedCost != null ? ` · Est. $${request.estimatedCost}` : ''}
                      {request.vendorName ? ` · ${request.vendorName}` : ''}
                    </Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Purchase Request</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Item Description"
              required
              fullWidth
              value={itemDescription}
              onChange={(e) => setItemDescription(e.target.value)}
            />
            <TextField
              label="Justification"
              required
              fullWidth
              multiline
              minRows={2}
              value={justification}
              onChange={(e) => setJustification(e.target.value)}
            />
            <TextField
              label="Estimated Cost"
              type="number"
              fullWidth
              value={estimatedCost}
              onChange={(e) => setEstimatedCost(e.target.value)}
            />
            <TextField label="Vendor" fullWidth value={vendorName} onChange={(e) => setVendorName(e.target.value)} />
            <FormControl fullWidth required>
              <InputLabel id="pr-approver-label">Approver</InputLabel>
              <Select
                labelId="pr-approver-label"
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
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={!itemDescription || !justification || !nominalApproverId || createRequest.isPending}
          >
            Submit
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!rejectTarget} onClose={() => setRejectTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Purchase Request</DialogTitle>
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
          <Button color="error" variant="contained" onClick={handleReject} disabled={!rejectReason || rejectRequest.isPending}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
