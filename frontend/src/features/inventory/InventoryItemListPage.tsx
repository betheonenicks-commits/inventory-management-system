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
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { useCreateInventoryItemMutation, useInventoryItemsQuery } from './hooks/useInventoryItemsQuery'
import { useLowStockQuery } from './hooks/useInventoryStockQuery'
import type { CostingMethod, UnitOfMeasure } from './types'

const UNITS_OF_MEASURE: UnitOfMeasure[] = ['EACH', 'BOX', 'KG', 'LITRE', 'PACK', 'ROLL']

/** US-INV-01/04/06/11: the item catalog, with a low-stock banner (US-INV-04) surfacing items that need reordering. */
export function InventoryItemListPage() {
  const navigate = useNavigate()
  const itemsQuery = useInventoryItemsQuery()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'inventory:write')
  const lowStockQuery = useLowStockQuery()
  const createItem = useCreateInventoryItemMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [name, setName] = useState('')
  const [sku, setSku] = useState('')
  const [unitOfMeasure, setUnitOfMeasure] = useState<UnitOfMeasure>('EACH')
  const [reorderLevel, setReorderLevel] = useState('')
  const [costingMethod, setCostingMethod] = useState<CostingMethod>('WEIGHTED_AVERAGE')
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setName('')
    setSku('')
    setUnitOfMeasure('EACH')
    setReorderLevel('')
    setCostingMethod('WEIGHTED_AVERAGE')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      const item = await createItem.mutateAsync({
        name,
        sku,
        unitOfMeasure,
        reorderLevel: reorderLevel ? Number(reorderLevel) : undefined,
        costingMethod,
      })
      setDialogOpen(false)
      navigate(`/inventory/items/${item.id}`)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create item')
    }
  }

  return (
    <Box>
      <PageHeader
        title="Inventory Items"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
              New Item
            </Button>
          )
        }
      />

      {lowStockQuery.isSuccess && lowStockQuery.data.length > 0 && (
        <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ mb: 2 }}>
          <Typography variant="body2">
            {lowStockQuery.data.length} item{lowStockQuery.data.length > 1 ? 's' : ''} below reorder level:{' '}
            {lowStockQuery.data.map((i) => `${i.name} (${i.totalQuantity}/${i.reorderLevel})`).join(', ')}
          </Typography>
        </Alert>
      )}

      {itemsQuery.isLoading && <LoadingSkeleton rows={6} />}
      {itemsQuery.isError && <ErrorPanel error={itemsQuery.error} onRetry={() => itemsQuery.refetch()} />}

      {itemsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {itemsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No inventory items yet.</Typography>
              </Box>
            )}
            {itemsQuery.data.map((item) => (
              <ListItemButton key={item.id} divider onClick={() => navigate(`/inventory/items/${item.id}`)}>
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{item.name}</Typography>
                      <Chip size="small" label={item.sku} variant="outlined" />
                      <Chip size="small" label={item.unitOfMeasure} />
                      {!item.active && <Chip size="small" label="Inactive" />}
                    </Stack>
                  }
                  secondary={item.reorderLevel != null ? `Reorder level: ${item.reorderLevel}` : undefined}
                />
              </ListItemButton>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Inventory Item</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" required fullWidth value={name} onChange={(e) => setName(e.target.value)} />
            <TextField label="SKU" required fullWidth value={sku} onChange={(e) => setSku(e.target.value)} />
            <FormControl fullWidth required>
              <InputLabel id="item-uom-label">Unit of Measure</InputLabel>
              <Select
                labelId="item-uom-label"
                label="Unit of Measure"
                value={unitOfMeasure}
                onChange={(e) => setUnitOfMeasure(e.target.value as UnitOfMeasure)}
              >
                {UNITS_OF_MEASURE.map((uom) => (
                  <MenuItem key={uom} value={uom}>
                    {uom}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Reorder Level"
              type="number"
              fullWidth
              value={reorderLevel}
              onChange={(e) => setReorderLevel(e.target.value)}
              helperText="Optional - leave blank to disable low-stock alerts for this item"
            />
            <FormControl fullWidth required>
              <InputLabel id="item-costing-label">Costing Method</InputLabel>
              <Select
                labelId="item-costing-label"
                label="Costing Method"
                value={costingMethod}
                onChange={(e) => setCostingMethod(e.target.value as CostingMethod)}
              >
                <MenuItem value="WEIGHTED_AVERAGE">Weighted Average</MenuItem>
                <MenuItem value="LAST_COST">Last Cost</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!name || !sku || createItem.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
