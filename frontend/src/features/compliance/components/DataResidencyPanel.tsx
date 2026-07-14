import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControlLabel from '@mui/material/FormControlLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../../api/errors'
import {
  useDataResidencyQuery,
  useOutboundFlowsQuery,
  useSaveOutboundFlowMutation,
} from '../hooks/useDataResidencyQuery'

/** US-CMP-05: single view confirming on-premises data residency and flagging enabled outbound flows. */
export function DataResidencyPanel({ canWrite }: { canWrite: boolean }) {
  const residencyQuery = useDataResidencyQuery()
  const flowsQuery = useOutboundFlowsQuery()
  const saveFlow = useSaveOutboundFlowMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [name, setName] = useState('')
  const [enabled, setEnabled] = useState(false)
  const [reviewNote, setReviewNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setName('')
    setEnabled(false)
    setReviewNote('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleSave() {
    setError(null)
    try {
      await saveFlow.mutateAsync({ name, enabled, complianceReviewNote: reviewNote || undefined })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save outbound flow')
    }
  }

  if (residencyQuery.isLoading || flowsQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (residencyQuery.isError) return <ErrorPanel error={residencyQuery.error} onRetry={() => residencyQuery.refetch()} />
  if (flowsQuery.isError) return <ErrorPanel error={flowsQuery.error} onRetry={() => flowsQuery.refetch()} />
  const residency = residencyQuery.data!

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Data Residency</Typography>

      {residency.allStoresOnPremises && residency.enabledOutboundFlows.length === 0 ? (
        <Alert severity="success" icon={<CheckCircleIcon fontSize="inherit" />}>
          All data stores are on-premises. No outbound flows are currently enabled.
        </Alert>
      ) : (
        <Alert severity={residency.allStoresOnPremises ? 'warning' : 'error'}>
          {residency.allStoresOnPremises
            ? `All data stores are on-premises, but ${residency.enabledOutboundFlows.length} outbound flow(s) are enabled.`
            : 'Data stores are not confirmed on-premises.'}
        </Alert>
      )}

      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="subtitle2">Outbound Integration Registry</Typography>
        {canWrite && (
          <Button size="small" variant="contained" onClick={openDialog}>
            Register Flow
          </Button>
        )}
      </Stack>

      <List dense>
        {(flowsQuery.data ?? []).map((flow) => (
          <ListItem key={flow.id} divider>
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Typography variant="body2">{flow.name}</Typography>
                  <Chip size="small" color={flow.enabled ? 'error' : 'default'} label={flow.enabled ? 'ENABLED' : 'DISABLED'} />
                </Stack>
              }
              secondary={flow.complianceReviewNote ?? undefined}
            />
          </ListItem>
        ))}
        {(flowsQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No outbound flows registered.
          </Typography>
        )}
      </List>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Register Outbound Flow</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              required
              fullWidth
              helperText="e.g. ACCOUNTING_EXPORT"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <FormControlLabel
              control={<Checkbox checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />}
              label="Enabled"
            />
            <TextField
              label="Compliance Review Note"
              fullWidth
              multiline
              minRows={2}
              value={reviewNote}
              onChange={(e) => setReviewNote(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={!name || saveFlow.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
