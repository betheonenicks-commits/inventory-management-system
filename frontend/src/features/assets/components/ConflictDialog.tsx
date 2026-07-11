import Dialog from '@mui/material/Dialog'
import DialogTitle from '@mui/material/DialogTitle'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogActions from '@mui/material/DialogActions'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import type { Asset } from '../types'

interface ConflictDialogProps {
  open: boolean
  localValues: Partial<Asset>
  serverResource: Asset | null
  onReload: () => void
  onCancel: () => void
}

const COMPARABLE_FIELDS: Array<{ key: keyof Asset; label: string }> = [
  { key: 'name', label: 'Name' },
  { key: 'manufacturer', label: 'Manufacturer' },
  { key: 'model', label: 'Model' },
  { key: 'serialNumber', label: 'Serial Number' },
  { key: 'purchaseCost', label: 'Purchase Cost' },
]

// A 409 optimistic-lock conflict never results in a silent overwrite - this
// dialog always shows both value sets and the only path forward is an
// explicit reload, per the reconciled UX spec's edit-conflict convention.
export function ConflictDialog({ open, localValues, serverResource, onReload, onCancel }: ConflictDialogProps) {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth="sm" fullWidth>
      <DialogTitle>This asset was changed by someone else</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Someone else saved a change to this asset while you were editing. Review what changed below, then reload
          to get the latest version before editing again - your in-progress changes were not saved.
        </DialogContentText>
        <Grid container spacing={2}>
          <Grid size={6}>
            <Typography variant="overline">Your version</Typography>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              {COMPARABLE_FIELDS.map((f) => (
                <Typography key={f.key} variant="body2">
                  <strong>{f.label}:</strong> {String(localValues[f.key] ?? '—')}
                </Typography>
              ))}
            </Paper>
          </Grid>
          <Grid size={6}>
            <Typography variant="overline">Current on server</Typography>
            <Paper variant="outlined" sx={{ p: 1.5 }}>
              {COMPARABLE_FIELDS.map((f) => (
                <Typography key={f.key} variant="body2">
                  <strong>{f.label}:</strong> {String(serverResource?.[f.key] ?? '—')}
                </Typography>
              ))}
            </Paper>
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Keep editing</Button>
        <Button onClick={onReload} variant="contained">
          Reload latest version
        </Button>
      </DialogActions>
    </Dialog>
  )
}
