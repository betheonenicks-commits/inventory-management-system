import { useState } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Collapse from '@mui/material/Collapse'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import {
  useCancelPurchaseOrderMutation,
  useLineEventsQuery,
  usePurchaseOrderLinesQuery,
  usePurchaseOrderQuery,
  useReceiveLineMutation,
  useReturnLineToVendorMutation,
} from './hooks/usePurchaseOrdersQuery'
import type { PurchaseOrderLineStatus, PurchaseOrderStatus } from './types'

const ORDER_STATUS_COLOR: Record<PurchaseOrderStatus, 'info' | 'success' | 'error'> = {
  OPEN: 'info',
  CLOSED: 'success',
  CANCELLED: 'error',
}

const LINE_STATUS_COLOR: Record<PurchaseOrderLineStatus, 'default' | 'warning' | 'success' | 'error'> = {
  OPEN: 'default',
  PARTIALLY_RECEIVED: 'warning',
  FULLY_RECEIVED: 'success',
  CANCELLED: 'error',
}

type DialogKind = 'receive' | 'return' | 'cancel-order' | null

export function PurchaseOrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const orderQuery = usePurchaseOrderQuery(orderId)
  const linesQuery = usePurchaseOrderLinesQuery(orderId)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')

  const receiveLine = useReceiveLineMutation(orderId ?? '')
  const returnLine = useReturnLineToVendorMutation(orderId ?? '')
  const cancelOrder = useCancelPurchaseOrderMutation(orderId ?? '')

  const [dialog, setDialog] = useState<DialogKind>(null)
  const [targetLineId, setTargetLineId] = useState<string | null>(null)
  const [expandedLineId, setExpandedLineId] = useState<string | null>(null)
  const [quantity, setQuantity] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  function close() {
    setDialog(null)
    setTargetLineId(null)
    setQuantity('')
    setNote('')
    setError(null)
  }

  async function handleReceive() {
    if (!targetLineId) return
    setError(null)
    try {
      await receiveLine.mutateAsync({ lineId: targetLineId, quantity: Number(quantity), discrepancyNote: note || undefined })
      close()
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to receive line')
    }
  }

  async function handleReturn() {
    if (!targetLineId) return
    setError(null)
    try {
      await returnLine.mutateAsync({ lineId: targetLineId, quantity: Number(quantity), reason: note })
      close()
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to log return to vendor')
    }
  }

  async function handleCancelOrder() {
    setError(null)
    try {
      await cancelOrder.mutateAsync(note)
      close()
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to cancel purchase order')
    }
  }

  if (orderQuery.isLoading) return <LoadingSkeleton rows={6} />
  if (orderQuery.isError) return <ErrorPanel error={orderQuery.error} onRetry={() => orderQuery.refetch()} />
  const order = orderQuery.data!

  return (
    <Box>
      <PageHeader
        title={order.poNumber}
        actions={
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Chip label={order.status} color={ORDER_STATUS_COLOR[order.status]} />
            {canWrite && order.status === 'OPEN' && (
              <Button size="small" color="error" onClick={() => setDialog('cancel-order')}>
                Cancel Order
              </Button>
            )}
          </Stack>
        }
      />

      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Vendor: {order.vendorName}
      </Typography>

      {error && dialog === null && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper variant="outlined">
        <List>
          {(linesQuery.data ?? []).map((line) => (
            <LineRow
              key={line.id}
              lineId={line.id}
              description={line.description}
              quantityOrdered={line.quantityOrdered}
              quantityReceived={line.quantityReceived}
              quantityReturned={line.quantityReturned}
              unitCost={line.unitCost}
              status={line.status}
              canWrite={canWrite}
              expanded={expandedLineId === line.id}
              onToggleExpand={() => setExpandedLineId((cur) => (cur === line.id ? null : line.id))}
              onReceive={() => { setTargetLineId(line.id); setDialog('receive') }}
              onReturn={() => { setTargetLineId(line.id); setDialog('return') }}
            />
          ))}
          {(linesQuery.data ?? []).length === 0 && (
            <Box sx={{ p: 3 }}>
              <Typography color="text.secondary">No line items.</Typography>
            </Box>
          )}
        </List>
      </Paper>

      <Dialog open={dialog === 'receive'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Receive Stock</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Quantity Received" type="number" required fullWidth value={quantity} onChange={(e) => setQuantity(e.target.value)} />
            <TextField label="Discrepancy Note" fullWidth value={note} onChange={(e) => setNote(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={handleReceive} disabled={!quantity || receiveLine.isPending}>
            Receive
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'return'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Return to Vendor</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Quantity" type="number" required fullWidth value={quantity} onChange={(e) => setQuantity(e.target.value)} />
            <TextField label="Reason" required fullWidth multiline minRows={2} value={note} onChange={(e) => setNote(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" color="warning" onClick={handleReturn} disabled={!quantity || !note || returnLine.isPending}>
            Log Return
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'cancel-order'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Cancel Purchase Order</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Only possible before any line has received stock.
          </Typography>
          <TextField label="Reason" required fullWidth multiline minRows={2} value={note} onChange={(e) => setNote(e.target.value)} />
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Keep Order</Button>
          <Button variant="contained" color="error" onClick={handleCancelOrder} disabled={!note || cancelOrder.isPending}>
            Cancel Order
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

interface LineRowProps {
  lineId: string
  description: string
  quantityOrdered: number
  quantityReceived: number
  quantityReturned: number
  unitCost: number
  status: PurchaseOrderLineStatus
  canWrite: boolean
  expanded: boolean
  onToggleExpand: () => void
  onReceive: () => void
  onReturn: () => void
}

function LineRow({
  lineId,
  description,
  quantityOrdered,
  quantityReceived,
  quantityReturned,
  unitCost,
  status,
  canWrite,
  expanded,
  onToggleExpand,
  onReceive,
  onReturn,
}: LineRowProps) {
  const eventsQuery = useLineEventsQuery(expanded ? lineId : undefined)

  return (
    <Box>
      <ListItem
        divider
        secondaryAction={
          canWrite && (
            <Stack direction="row" spacing={0.5}>
              {(status === 'OPEN' || status === 'PARTIALLY_RECEIVED') && (
                <Button size="small" onClick={onReceive}>
                  Receive
                </Button>
              )}
              {quantityReceived > quantityReturned && (
                <Button size="small" color="warning" onClick={onReturn}>
                  Return
                </Button>
              )}
              <Button size="small" color="inherit" onClick={onToggleExpand}>
                {expanded ? 'Hide history' : 'History'}
              </Button>
            </Stack>
          )
        }
      >
        <ListItemText
          primary={
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <Typography variant="body2">{description}</Typography>
              <Chip size="small" label={status.replace(/_/g, ' ')} color={LINE_STATUS_COLOR[status]} />
            </Stack>
          }
          secondary={`${quantityReceived}/${quantityOrdered} received${quantityReturned > 0 ? `, ${quantityReturned} returned` : ''} · $${unitCost}/unit`}
        />
      </ListItem>
      <Collapse in={expanded}>
        <Box sx={{ pl: 4, pr: 2, pb: 1 }}>
          {(eventsQuery.data ?? []).map((event) => (
            <Typography key={event.id} variant="caption" color="text.secondary" component="div">
              {event.eventType}
              {event.quantity != null ? ` (${event.quantity})` : ''}
              {event.note ? ` — ${event.note}` : ''} · {new Date(event.createdAt).toLocaleString()}
            </Typography>
          ))}
          {(eventsQuery.data ?? []).length === 0 && (
            <Typography variant="caption" color="text.secondary">
              No events yet.
            </Typography>
          )}
        </Box>
      </Collapse>
    </Box>
  )
}
