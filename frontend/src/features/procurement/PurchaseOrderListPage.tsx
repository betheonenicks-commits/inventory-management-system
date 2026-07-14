import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import IconButton from '@mui/material/IconButton'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { usePurchaseRequestsQuery } from './hooks/usePurchaseRequestsQuery'
import { useCreatePurchaseOrderMutation, usePurchaseOrdersQuery } from './hooks/usePurchaseOrdersQuery'
import type { PurchaseOrderStatus } from './types'

const STATUS_COLOR: Record<PurchaseOrderStatus, 'info' | 'success' | 'error'> = {
  OPEN: 'info',
  CLOSED: 'success',
  CANCELLED: 'error',
}

interface LineDraft {
  description: string
  quantityOrdered: string
  unitCost: string
}

const EMPTY_LINE: LineDraft = { description: '', quantityOrdered: '1', unitCost: '' }

/** US-LIF-02: create a PO from an approved purchase request. */
export function PurchaseOrderListPage() {
  const navigate = useNavigate()
  const ordersQuery = usePurchaseOrdersQuery()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')
  const approvedRequestsQuery = usePurchaseRequestsQuery('APPROVED')
  const createOrder = useCreatePurchaseOrderMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [purchaseRequestId, setPurchaseRequestId] = useState('')
  const [vendorName, setVendorName] = useState('')
  const [lines, setLines] = useState<LineDraft[]>([{ ...EMPTY_LINE }])
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setPurchaseRequestId('')
    setVendorName('')
    setLines([{ ...EMPTY_LINE }])
    setError(null)
    setDialogOpen(true)
  }

  function updateLine(index: number, patch: Partial<LineDraft>) {
    setLines((ls) => ls.map((l, i) => (i === index ? { ...l, ...patch } : l)))
  }

  async function handleCreate() {
    setError(null)
    try {
      const order = await createOrder.mutateAsync({
        purchaseRequestId,
        vendorName,
        lines: lines.map((l) => ({
          description: l.description,
          quantityOrdered: Number(l.quantityOrdered),
          unitCost: Number(l.unitCost),
        })),
      })
      setDialogOpen(false)
      navigate(`/procurement/purchase-orders/${order.id}`)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create purchase order')
    }
  }

  const canSubmit =
    !!purchaseRequestId && !!vendorName && lines.every((l) => l.description && Number(l.quantityOrdered) > 0 && l.unitCost)

  return (
    <Box>
      <PageHeader
        title="Purchase Orders"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
              New Purchase Order
            </Button>
          )
        }
      />

      {ordersQuery.isLoading && <LoadingSkeleton rows={6} />}
      {ordersQuery.isError && <ErrorPanel error={ordersQuery.error} onRetry={() => ordersQuery.refetch()} />}

      {ordersQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {ordersQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No purchase orders yet.</Typography>
              </Box>
            )}
            {ordersQuery.data.map((order) => (
              <ListItemButton key={order.id} divider onClick={() => navigate(`/procurement/purchase-orders/${order.id}`)}>
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{order.poNumber}</Typography>
                      <Chip size="small" label={order.status} color={STATUS_COLOR[order.status]} />
                    </Stack>
                  }
                  secondary={order.vendorName}
                />
              </ListItemButton>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Purchase Order</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel id="po-request-label">Approved Purchase Request</InputLabel>
              <Select
                labelId="po-request-label"
                label="Approved Purchase Request"
                value={purchaseRequestId}
                onChange={(e) => setPurchaseRequestId(e.target.value)}
              >
                {(approvedRequestsQuery.data ?? []).map((request) => (
                  <MenuItem key={request.id} value={request.id}>
                    {request.itemDescription}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField label="Vendor" required fullWidth value={vendorName} onChange={(e) => setVendorName(e.target.value)} />

            <Typography variant="subtitle2">Line Items</Typography>
            {lines.map((line, index) => (
              <Stack key={index} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                <TextField
                  size="small"
                  label="Description"
                  value={line.description}
                  onChange={(e) => updateLine(index, { description: e.target.value })}
                  sx={{ flexGrow: 1 }}
                />
                <TextField
                  size="small"
                  label="Qty"
                  type="number"
                  value={line.quantityOrdered}
                  onChange={(e) => updateLine(index, { quantityOrdered: e.target.value })}
                  sx={{ width: 90 }}
                />
                <TextField
                  size="small"
                  label="Unit Cost"
                  type="number"
                  value={line.unitCost}
                  onChange={(e) => updateLine(index, { unitCost: e.target.value })}
                  sx={{ width: 110 }}
                />
                {lines.length > 1 && (
                  <IconButton size="small" onClick={() => setLines((ls) => ls.filter((_, i) => i !== index))}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                )}
              </Stack>
            ))}
            <Button size="small" onClick={() => setLines((ls) => [...ls, { ...EMPTY_LINE }])} sx={{ alignSelf: 'flex-start' }}>
              + Add line
            </Button>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!canSubmit || createOrder.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
