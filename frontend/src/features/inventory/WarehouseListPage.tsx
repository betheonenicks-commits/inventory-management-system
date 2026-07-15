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
import { useOrgNodesQuery } from '../audits/hooks/useOrgNodesQuery'
import { useCreateWarehouseMutation, useDeactivateWarehouseMutation, useWarehousesQuery } from './hooks/useWarehousesQuery'

/** US-INV-03: warehouses, org-scoped exactly like assets - shelf/bin sub-location lives on stock balances, not here. */
export function WarehouseListPage() {
  const warehousesQuery = useWarehousesQuery()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'inventory:write')
  const orgNodesQuery = useOrgNodesQuery()
  const createWarehouse = useCreateWarehouseMutation()
  const deactivateWarehouse = useDeactivateWarehouseMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [orgNodeId, setOrgNodeId] = useState('')
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setName('')
    setCode('')
    setOrgNodeId('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await createWarehouse.mutateAsync({ name, code, orgNodeId })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create warehouse')
    }
  }

  async function handleDeactivate(id: string) {
    setError(null)
    try {
      await deactivateWarehouse.mutateAsync(id)
    } catch (err) {
      // AC-INV-03-X: blocked while stock is still on hand - the backend's own message names it precisely.
      setError(isApiProblem(err) ? err.detail : 'Failed to deactivate warehouse')
    }
  }

  return (
    <Box>
      <PageHeader
        title="Warehouses"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
              New Warehouse
            </Button>
          )
        }
      />

      {error && !dialogOpen && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {warehousesQuery.isLoading && <LoadingSkeleton rows={4} />}
      {warehousesQuery.isError && <ErrorPanel error={warehousesQuery.error} onRetry={() => warehousesQuery.refetch()} />}

      {warehousesQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {warehousesQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No warehouses yet.</Typography>
              </Box>
            )}
            {warehousesQuery.data.map((warehouse) => (
              <ListItem
                key={warehouse.id}
                divider
                secondaryAction={
                  canWrite &&
                  warehouse.active && (
                    <Button size="small" color="error" onClick={() => handleDeactivate(warehouse.id)}>
                      Deactivate
                    </Button>
                  )
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{warehouse.name}</Typography>
                      <Chip size="small" label={warehouse.code} variant="outlined" />
                      {!warehouse.active && <Chip size="small" label="Inactive" />}
                    </Stack>
                  }
                  secondary={warehouse.orgNodeName}
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Warehouse</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" required fullWidth value={name} onChange={(e) => setName(e.target.value)} />
            <TextField label="Code" required fullWidth value={code} onChange={(e) => setCode(e.target.value)} />
            <FormControl fullWidth required>
              <InputLabel id="warehouse-org-node-label">Location</InputLabel>
              <Select
                labelId="warehouse-org-node-label"
                label="Location"
                value={orgNodeId}
                onChange={(e) => setOrgNodeId(e.target.value)}
              >
                {(orgNodesQuery.data ?? []).map((node) => (
                  <MenuItem key={node.id} value={node.id}>
                    {node.name} ({node.levelName})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!name || !code || !orgNodeId || createWarehouse.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
