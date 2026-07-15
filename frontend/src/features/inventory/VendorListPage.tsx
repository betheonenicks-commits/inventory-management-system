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
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import {
  useCreateVendorMutation,
  useDeactivateVendorMutation,
  useVendorPurchaseOrdersQuery,
  useVendorsQuery,
} from './hooks/useVendorsQuery'

/** US-INV-07/08: vendor CRUD, independent of items, plus full purchase-order history per vendor. */
export function VendorListPage() {
  const vendorsQuery = useVendorsQuery()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'inventory:write')
  const createVendor = useCreateVendorMutation()
  const deactivateVendor = useDeactivateVendorMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [name, setName] = useState('')
  const [contactEmail, setContactEmail] = useState('')
  const [contactPhone, setContactPhone] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [historyVendorId, setHistoryVendorId] = useState<string | null>(null)
  const historyQuery = useVendorPurchaseOrdersQuery(historyVendorId ?? undefined, !!historyVendorId)

  function openDialog() {
    setName('')
    setContactEmail('')
    setContactPhone('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      await createVendor.mutateAsync({ name, contactEmail: contactEmail || undefined, contactPhone: contactPhone || undefined })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create vendor')
    }
  }

  async function handleDeactivate(id: string) {
    setError(null)
    try {
      await deactivateVendor.mutateAsync(id)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to deactivate vendor')
    }
  }

  return (
    <Box>
      <PageHeader
        title="Vendors"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
              New Vendor
            </Button>
          )
        }
      />

      {error && !dialogOpen && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {vendorsQuery.isLoading && <LoadingSkeleton rows={4} />}
      {vendorsQuery.isError && <ErrorPanel error={vendorsQuery.error} onRetry={() => vendorsQuery.refetch()} />}

      {vendorsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {vendorsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No vendors yet.</Typography>
              </Box>
            )}
            {vendorsQuery.data.map((vendor) => (
              <ListItem
                key={vendor.id}
                divider
                secondaryAction={
                  canWrite &&
                  vendor.active && (
                    <Button size="small" color="error" onClick={() => handleDeactivate(vendor.id)}>
                      Deactivate
                    </Button>
                  )
                }
                disablePadding
              >
                <ListItemButton onClick={() => setHistoryVendorId(vendor.id)}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Typography variant="body1">{vendor.name}</Typography>
                        {!vendor.active && <Chip size="small" label="Inactive" />}
                      </Stack>
                    }
                    secondary={[vendor.contactEmail, vendor.contactPhone].filter(Boolean).join(' · ') || undefined}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Vendor</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" required fullWidth value={name} onChange={(e) => setName(e.target.value)} />
            <TextField label="Contact Email" fullWidth value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} />
            <TextField label="Contact Phone" fullWidth value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!name || createVendor.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!historyVendorId} onClose={() => setHistoryVendorId(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Purchase History</DialogTitle>
        <DialogContent>
          {historyQuery.isLoading && <LoadingSkeleton rows={3} />}
          {historyQuery.isSuccess && historyQuery.data.length === 0 && (
            <Typography color="text.secondary">No purchase orders for this vendor yet.</Typography>
          )}
          <List dense>
            {(historyQuery.data ?? []).map((order) => (
              <ListItem key={order.id} divider>
                <ListItemText primary={order.poNumber} secondary={order.status} />
              </ListItem>
            ))}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setHistoryVendorId(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
