import { useState } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import Grid from '@mui/material/Grid'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { useWarehousesQuery } from './hooks/useWarehousesQuery'
import {
  useCostingMethodHistoryQuery,
  useInventoryItemQuery,
  useUpdateInventoryItemMutation,
} from './hooks/useInventoryItemsQuery'
import {
  useBalancesQuery,
  useStockInMutation,
  useStockOutMutation,
  useTransactionsQuery,
  useTransferStockMutation,
} from './hooks/useInventoryStockQuery'
import type { CostingMethod } from './types'

type DialogKind = 'edit' | 'stock-in' | 'stock-out' | 'transfer' | null

function describeError(err: unknown, fallback: string): string {
  if (!isApiProblem(err)) return fallback
  if (err.errors && err.errors.length > 0) return err.errors.map((e) => e.message).join(' ')
  return err.detail
}

/** US-INV-01/02/06/08/09/10/11: one item's balances across every warehouse/sub-location/lot, its movement history, and the actions that change it. */
export function InventoryItemDetailPage() {
  const { itemId } = useParams<{ itemId: string }>()
  const itemQuery = useInventoryItemQuery(itemId)
  const balancesQuery = useBalancesQuery(itemId)
  const transactionsQuery = useTransactionsQuery(itemId)
  const costingHistoryQuery = useCostingMethodHistoryQuery(itemId)
  const warehousesQuery = useWarehousesQuery()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'inventory:write')

  const updateItem = useUpdateInventoryItemMutation(itemId ?? '')
  const stockIn = useStockInMutation()
  const stockOut = useStockOutMutation()
  const transferStock = useTransferStockMutation()

  const [dialog, setDialog] = useState<DialogKind>(null)
  const [error, setError] = useState<string | null>(null)

  const [editName, setEditName] = useState('')
  const [editReorderLevel, setEditReorderLevel] = useState('')
  const [editCostingMethod, setEditCostingMethod] = useState<CostingMethod>('WEIGHTED_AVERAGE')

  const [warehouseId, setWarehouseId] = useState('')
  const [toWarehouseId, setToWarehouseId] = useState('')
  const [subLocation, setSubLocation] = useState('')
  const [toSubLocation, setToSubLocation] = useState('')
  const [lotNumber, setLotNumber] = useState('')
  const [toLotNumber, setToLotNumber] = useState('')
  const [expiryDate, setExpiryDate] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unitCost, setUnitCost] = useState('')
  const [currencyCode, setCurrencyCode] = useState('')
  const [fxRate, setFxRate] = useState('')
  const [reasonCode, setReasonCode] = useState('')

  function closeDialog() {
    setDialog(null)
    setError(null)
    setWarehouseId('')
    setToWarehouseId('')
    setSubLocation('')
    setToSubLocation('')
    setLotNumber('')
    setToLotNumber('')
    setExpiryDate('')
    setQuantity('')
    setUnitCost('')
    setCurrencyCode('')
    setFxRate('')
    setReasonCode('')
  }

  function openEdit() {
    if (!itemQuery.data) return
    setEditName(itemQuery.data.name)
    setEditReorderLevel(itemQuery.data.reorderLevel != null ? String(itemQuery.data.reorderLevel) : '')
    setEditCostingMethod(itemQuery.data.costingMethod)
    setError(null)
    setDialog('edit')
  }

  async function handleEdit() {
    setError(null)
    try {
      await updateItem.mutateAsync({
        name: editName,
        reorderLevel: editReorderLevel ? Number(editReorderLevel) : undefined,
        costingMethod: editCostingMethod,
      })
      closeDialog()
    } catch (err) {
      setError(describeError(err, 'Failed to update item'))
    }
  }

  async function handleStockIn() {
    if (!itemId) return
    setError(null)
    try {
      await stockIn.mutateAsync({
        itemId,
        warehouseId,
        subLocation: subLocation || undefined,
        lotNumber: lotNumber || undefined,
        expiryDate: expiryDate || undefined,
        quantity: Number(quantity),
        unitCost: Number(unitCost),
        currencyCode: currencyCode || undefined,
        fxRate: fxRate ? Number(fxRate) : undefined,
        reasonCode,
      })
      closeDialog()
    } catch (err) {
      setError(describeError(err, 'Failed to record stock in'))
    }
  }

  async function handleStockOut() {
    if (!itemId) return
    setError(null)
    try {
      await stockOut.mutateAsync({
        itemId,
        warehouseId,
        subLocation: subLocation || undefined,
        lotNumber: lotNumber || undefined,
        quantity: Number(quantity),
        reasonCode,
      })
      closeDialog()
    } catch (err) {
      setError(describeError(err, 'Failed to record stock out'))
    }
  }

  async function handleTransfer() {
    if (!itemId) return
    setError(null)
    try {
      await transferStock.mutateAsync({
        itemId,
        fromWarehouseId: warehouseId,
        fromSubLocation: subLocation || undefined,
        fromLotNumber: lotNumber || undefined,
        toWarehouseId,
        toSubLocation: toSubLocation || undefined,
        toLotNumber: toLotNumber || undefined,
        quantity: Number(quantity),
        reasonCode,
      })
      closeDialog()
    } catch (err) {
      setError(describeError(err, 'Failed to transfer stock'))
    }
  }

  if (itemQuery.isLoading) return <LoadingSkeleton rows={6} />
  if (itemQuery.isError) return <ErrorPanel error={itemQuery.error} onRetry={() => itemQuery.refetch()} />
  const item = itemQuery.data!

  return (
    <Box>
      <PageHeader
        title={item.name}
        actions={
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Chip label={item.sku} variant="outlined" />
            <Chip label={item.unitOfMeasure} />
            <Chip label={item.costingMethod.replace('_', ' ')} color="info" />
            {!item.active && <Chip label="Inactive" />}
            {canWrite && (
              <Button size="small" onClick={openEdit}>
                Edit
              </Button>
            )}
          </Stack>
        }
      />

      {canWrite && (
        <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
          <Button variant="contained" size="small" onClick={() => setDialog('stock-in')}>
            Stock In
          </Button>
          <Button variant="outlined" size="small" onClick={() => setDialog('stock-out')}>
            Stock Out
          </Button>
          <Button variant="outlined" size="small" onClick={() => setDialog('transfer')}>
            Transfer
          </Button>
        </Stack>
      )}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Stock Balances
            </Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Warehouse</TableCell>
                    <TableCell>Sub-location</TableCell>
                    <TableCell>Lot</TableCell>
                    <TableCell>Expiry</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Avg Cost</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(balancesQuery.data ?? []).map((balance) => (
                    <TableRow key={balance.id}>
                      <TableCell>{balance.warehouseName}</TableCell>
                      <TableCell>{balance.subLocation || '—'}</TableCell>
                      <TableCell>{balance.lotNumber || '—'}</TableCell>
                      <TableCell>{balance.expiryDate ?? '—'}</TableCell>
                      <TableCell align="right">
                        {balance.quantityOnHand} {item.unitOfMeasure}
                      </TableCell>
                      <TableCell align="right">{balance.averageUnitCost ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                  {(balancesQuery.data ?? []).length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6}>
                        <Typography color="text.secondary" variant="body2">
                          No stock recorded yet.
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>

          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Movement History
            </Typography>
            <List dense>
              {(transactionsQuery.data ?? []).map((tx) => (
                <ListItem key={tx.id} divider>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Chip size="small" label={tx.transactionType.replace('_', ' ')} />
                        <Typography variant="body2">
                          {tx.quantity} {item.unitOfMeasure} @ {tx.warehouseName}
                        </Typography>
                      </Stack>
                    }
                    secondary={`${tx.reasonCode} · ${tx.performedByUsername} · ${new Date(tx.performedAt).toLocaleString()}`}
                  />
                </ListItem>
              ))}
              {(transactionsQuery.data ?? []).length === 0 && (
                <Typography color="text.secondary" variant="body2">
                  No movements recorded yet.
                </Typography>
              )}
            </List>
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Costing Method History
            </Typography>
            <List dense>
              {(costingHistoryQuery.data ?? []).map((change) => (
                <ListItem key={change.id} divider>
                  <ListItemText
                    primary={`${change.oldMethod.replace('_', ' ')} → ${change.newMethod.replace('_', ' ')}`}
                    secondary={new Date(change.changedAt).toLocaleString()}
                  />
                </ListItem>
              ))}
              {(costingHistoryQuery.data ?? []).length === 0 && (
                <Typography color="text.secondary" variant="body2">
                  No costing method changes yet.
                </Typography>
              )}
            </List>
          </Paper>
        </Grid>
      </Grid>

      <Dialog open={dialog === 'edit'} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Edit {item.name}</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" required fullWidth value={editName} onChange={(e) => setEditName(e.target.value)} />
            <TextField
              label="Reorder Level"
              type="number"
              fullWidth
              value={editReorderLevel}
              onChange={(e) => setEditReorderLevel(e.target.value)}
            />
            <FormControl fullWidth required>
              <InputLabel id="edit-costing-label">Costing Method</InputLabel>
              <Select
                labelId="edit-costing-label"
                label="Costing Method"
                value={editCostingMethod}
                onChange={(e) => setEditCostingMethod(e.target.value as CostingMethod)}
              >
                <MenuItem value="WEIGHTED_AVERAGE">Weighted Average</MenuItem>
                <MenuItem value="LAST_COST">Last Cost</MenuItem>
              </Select>
            </FormControl>
            <Typography variant="caption" color="text.secondary">
              Switching costing method only affects future receipts and is recorded in the history on the right.
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleEdit} disabled={!editName || updateItem.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'stock-in'} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Stock In</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel id="stock-in-warehouse-label">Warehouse</InputLabel>
              <Select labelId="stock-in-warehouse-label" label="Warehouse" value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)}>
                {(warehousesQuery.data ?? []).map((w) => (
                  <MenuItem key={w.id} value={w.id}>
                    {w.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Stack direction="row" spacing={2}>
              <TextField label="Sub-location" fullWidth value={subLocation} onChange={(e) => setSubLocation(e.target.value)} />
              <TextField label="Lot Number" fullWidth value={lotNumber} onChange={(e) => setLotNumber(e.target.value)} />
            </Stack>
            <TextField
              label="Expiry Date"
              type="date"
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
            />
            <Stack direction="row" spacing={2}>
              <TextField label="Quantity" type="number" required fullWidth value={quantity} onChange={(e) => setQuantity(e.target.value)} />
              <TextField label="Unit Cost" type="number" required fullWidth value={unitCost} onChange={(e) => setUnitCost(e.target.value)} />
            </Stack>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Currency (blank = USD)"
                fullWidth
                value={currencyCode}
                onChange={(e) => setCurrencyCode(e.target.value.toUpperCase())}
              />
              <TextField label="FX Rate" type="number" fullWidth value={fxRate} onChange={(e) => setFxRate(e.target.value)} />
            </Stack>
            <TextField label="Reason" required fullWidth value={reasonCode} onChange={(e) => setReasonCode(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleStockIn}
            disabled={!warehouseId || !quantity || !unitCost || !reasonCode || stockIn.isPending}
          >
            Record
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'stock-out'} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Stock Out</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel id="stock-out-warehouse-label">Warehouse</InputLabel>
              <Select labelId="stock-out-warehouse-label" label="Warehouse" value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)}>
                {(warehousesQuery.data ?? []).map((w) => (
                  <MenuItem key={w.id} value={w.id}>
                    {w.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Stack direction="row" spacing={2}>
              <TextField label="Sub-location" fullWidth value={subLocation} onChange={(e) => setSubLocation(e.target.value)} />
              <TextField label="Lot Number" fullWidth value={lotNumber} onChange={(e) => setLotNumber(e.target.value)} />
            </Stack>
            <TextField label="Quantity" type="number" required fullWidth value={quantity} onChange={(e) => setQuantity(e.target.value)} />
            <TextField label="Reason" required fullWidth value={reasonCode} onChange={(e) => setReasonCode(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleStockOut} disabled={!warehouseId || !quantity || !reasonCode || stockOut.isPending}>
            Record
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'transfer'} onClose={closeDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Transfer Between Warehouses</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth required>
              <InputLabel id="transfer-from-label">From Warehouse</InputLabel>
              <Select labelId="transfer-from-label" label="From Warehouse" value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)}>
                {(warehousesQuery.data ?? []).map((w) => (
                  <MenuItem key={w.id} value={w.id}>
                    {w.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Stack direction="row" spacing={2}>
              <TextField label="From Sub-location" fullWidth value={subLocation} onChange={(e) => setSubLocation(e.target.value)} />
              <TextField label="From Lot" fullWidth value={lotNumber} onChange={(e) => setLotNumber(e.target.value)} />
            </Stack>
            <FormControl fullWidth required>
              <InputLabel id="transfer-to-label">To Warehouse</InputLabel>
              <Select labelId="transfer-to-label" label="To Warehouse" value={toWarehouseId} onChange={(e) => setToWarehouseId(e.target.value)}>
                {(warehousesQuery.data ?? []).map((w) => (
                  <MenuItem key={w.id} value={w.id}>
                    {w.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Stack direction="row" spacing={2}>
              <TextField label="To Sub-location" fullWidth value={toSubLocation} onChange={(e) => setToSubLocation(e.target.value)} />
              <TextField label="To Lot" fullWidth value={toLotNumber} onChange={(e) => setToLotNumber(e.target.value)} />
            </Stack>
            <TextField label="Quantity" type="number" required fullWidth value={quantity} onChange={(e) => setQuantity(e.target.value)} />
            <TextField label="Reason" required fullWidth value={reasonCode} onChange={(e) => setReasonCode(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleTransfer}
            disabled={!warehouseId || !toWarehouseId || !quantity || !reasonCode || transferStock.isPending}
          >
            Transfer
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
