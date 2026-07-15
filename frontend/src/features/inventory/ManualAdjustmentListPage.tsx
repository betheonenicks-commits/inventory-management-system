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
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { usePickableUsersQuery } from '../users/hooks/useUsersQuery'
import { useInventoryItemsQuery } from './hooks/useInventoryItemsQuery'
import { useWarehousesQuery } from './hooks/useWarehousesQuery'
import {
  useApproveManualAdjustmentMutation,
  useManualAdjustmentsQuery,
  useRejectManualAdjustmentMutation,
  useRequestManualAdjustmentMutation,
} from './hooks/useManualAdjustmentsQuery'
import type { LifecycleRequestStatus } from '../lifecycle/types'

const STATUS_COLOR: Record<LifecycleRequestStatus, 'warning' | 'success' | 'error'> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

function describeError(err: unknown, fallback: string): string {
  if (!isApiProblem(err)) return fallback
  if (err.errors && err.errors.length > 0) return err.errors.map((e) => e.message).join(' ')
  return err.detail
}

/** US-INV-05: a manual quantity correction requires a reason and routed approval before it changes anything. */
export function ManualAdjustmentListPage() {
  const adjustmentsQuery = useManualAdjustmentsQuery()
  const user = useAuthStore((s) => s.user)
  const canRequest = hasPermission(user, 'inventory:write')
  const canDecide = hasPermission(user, 'approvals:write')

  const [createOpen, setCreateOpen] = useState(false)
  // All three pickers are only fetched once the create dialog is actually open - a
  // Department Head (approvals:read/write, no inventory:read) can reach this whole
  // page to review/decide on requests routed to them, but must never trigger the
  // inventory:read-gated items/warehouses lookups just from opening the page.
  const itemsQuery = useInventoryItemsQuery(true, createOpen)
  const warehousesQuery = useWarehousesQuery(createOpen)
  const usersQuery = usePickableUsersQuery(createOpen)
  const requestAdjustment = useRequestManualAdjustmentMutation()

  const [itemId, setItemId] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const [quantityDelta, setQuantityDelta] = useState('')
  const [reason, setReason] = useState('')
  const [nominalApproverId, setNominalApproverId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [rejectTargetId, setRejectTargetId] = useState<string | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [rejectError, setRejectError] = useState<string | null>(null)
  const rejectAdjustment = useRejectManualAdjustmentMutation(rejectTargetId ?? '')

  async function handleReject() {
    if (!rejectTargetId) return
    setRejectError(null)
    try {
      await rejectAdjustment.mutateAsync(rejectReason)
      setRejectTargetId(null)
    } catch (err) {
      setRejectError(describeError(err, 'Failed to reject adjustment'))
    }
  }

  function openCreate() {
    setItemId('')
    setWarehouseId('')
    setQuantityDelta('')
    setReason('')
    setNominalApproverId('')
    setError(null)
    setCreateOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await requestAdjustment.mutateAsync({
        itemId,
        warehouseId,
        quantityDelta: Number(quantityDelta),
        reason,
        nominalApproverId,
      })
      setCreateOpen(false)
    } catch (err) {
      setError(describeError(err, 'Failed to submit adjustment request'))
    }
  }

  return (
    <Box>
      <PageHeader
        title="Inventory Adjustments"
        actions={
          canRequest && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
              Request Adjustment
            </Button>
          )
        }
      />

      {adjustmentsQuery.isLoading && <LoadingSkeleton rows={4} />}
      {adjustmentsQuery.isError && <ErrorPanel error={adjustmentsQuery.error} onRetry={() => adjustmentsQuery.refetch()} />}

      {adjustmentsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {adjustmentsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No adjustment requests yet.</Typography>
              </Box>
            )}
            {adjustmentsQuery.data.map((adjustment) => (
              <AdjustmentRow
                key={adjustment.id}
                id={adjustment.id}
                itemName={adjustment.itemName}
                warehouseName={adjustment.warehouseName}
                quantityDelta={adjustment.quantityDelta}
                reason={adjustment.reason}
                status={adjustment.status}
                rejectionReason={adjustment.rejectionReason}
                canDecide={canDecide}
                onReject={() => {
                  setRejectTargetId(adjustment.id)
                  setRejectReason('')
                  setRejectError(null)
                }}
              />
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Request Manual Adjustment</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel id="adj-item-label">Item</InputLabel>
              <Select labelId="adj-item-label" label="Item" value={itemId} onChange={(e) => setItemId(e.target.value)}>
                {(itemsQuery.data ?? []).map((item) => (
                  <MenuItem key={item.id} value={item.id}>
                    {item.name} ({item.sku})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth required>
              <InputLabel id="adj-warehouse-label">Warehouse</InputLabel>
              <Select labelId="adj-warehouse-label" label="Warehouse" value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)}>
                {(warehousesQuery.data ?? []).map((w) => (
                  <MenuItem key={w.id} value={w.id}>
                    {w.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Quantity Change"
              type="number"
              required
              fullWidth
              helperText="Negative for a shortfall (e.g. a recount found fewer units), positive to increase"
              value={quantityDelta}
              onChange={(e) => setQuantityDelta(e.target.value)}
            />
            <TextField
              label="Reason"
              required
              fullWidth
              multiline
              minRows={2}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <FormControl fullWidth required>
              <InputLabel id="adj-approver-label">Approver</InputLabel>
              <Select
                labelId="adj-approver-label"
                label="Approver"
                value={nominalApproverId}
                onChange={(e) => setNominalApproverId(e.target.value)}
              >
                {(usersQuery.data ?? []).map((u) => (
                  <MenuItem key={u.id} value={u.id}>
                    {u.displayName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={!itemId || !warehouseId || !quantityDelta || !reason || !nominalApproverId || requestAdjustment.isPending}
          >
            Submit
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!rejectTargetId} onClose={() => setRejectTargetId(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Adjustment</DialogTitle>
        <DialogContent>
          {rejectError && <Alert severity="error" sx={{ mb: 2 }}>{rejectError}</Alert>}
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
          <Button onClick={() => setRejectTargetId(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleReject} disabled={!rejectReason || rejectAdjustment.isPending}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

interface AdjustmentRowProps {
  id: string
  itemName: string
  warehouseName: string
  quantityDelta: number
  reason: string
  status: LifecycleRequestStatus
  rejectionReason: string | null
  canDecide: boolean
  onReject: () => void
}

function AdjustmentRow({ id, itemName, warehouseName, quantityDelta, reason, status, rejectionReason, canDecide, onReject }: AdjustmentRowProps) {
  const approve = useApproveManualAdjustmentMutation(id)
  const [error, setError] = useState<string | null>(null)

  async function handleApprove() {
    setError(null)
    try {
      await approve.mutateAsync()
    } catch (err) {
      setError(describeError(err, 'Failed to approve'))
    }
  }

  return (
    <ListItem
      divider
      secondaryAction={
        canDecide &&
        status === 'PENDING' && (
          <Stack direction="row" spacing={1}>
            <Button size="small" color="success" onClick={handleApprove} disabled={approve.isPending}>
              Approve
            </Button>
            <Button size="small" color="error" onClick={onReject}>
              Reject
            </Button>
          </Stack>
        )
      }
    >
      <ListItemText
        primary={
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Typography variant="body2">
              {itemName} @ {warehouseName}
            </Typography>
            <Chip size="small" label={quantityDelta > 0 ? `+${quantityDelta}` : quantityDelta} />
            <Chip size="small" label={status} color={STATUS_COLOR[status]} />
          </Stack>
        }
        secondary={
          error
            ? error
            : status === 'REJECTED' && rejectionReason
              ? `${reason} — rejected: ${rejectionReason}`
              : reason
        }
      />
    </ListItem>
  )
}
