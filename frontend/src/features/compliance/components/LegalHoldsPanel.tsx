import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
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
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../../api/errors'
import { fetchAssets } from '../../../api/assets/assetApi'
import { useQuery } from '@tanstack/react-query'
import { useAuditsQuery } from '../../audits/hooks/useAuditsQuery'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { useLegalHoldsQuery, useLiftLegalHoldMutation, usePlaceLegalHoldMutation } from '../hooks/useLegalHoldsQuery'
import type { LegalHoldScopeType } from '../types'

/** US-CMP-06: place/lift a legal hold on an asset or audit record. */
export function LegalHoldsPanel({ canWrite }: { canWrite: boolean }) {
  const holdsQuery = useLegalHoldsQuery()
  const [scopeType, setScopeType] = useState<LegalHoldScopeType>('ASSET')
  const [dialogOpen, setDialogOpen] = useState(false)
  // Only fetched when actually needed (dialog open + Audit scope picked) - a
  // Compliance Officer has no audits:read at all, so fetching this
  // unconditionally would 403 just from opening this panel, the same bug
  // class the EPIC-AUD frontend session found and fixed for useUsersQuery.
  const auditsQuery = useAuditsQuery(undefined, dialogOpen && scopeType === 'AUDIT')
  const placeHold = usePlaceLegalHoldMutation()
  const liftHold = useLiftLegalHoldMutation()

  const [assetSearch, setAssetSearch] = useState('')
  const [selectedAssetId, setSelectedAssetId] = useState('')
  const [selectedAuditId, setSelectedAuditId] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [liftTarget, setLiftTarget] = useState<string | null>(null)
  const [liftReason, setLiftReason] = useState('')

  const assetSearchQuery = useQuery({
    queryKey: ['CMP', 'legalHoldAssetSearch', assetSearch],
    queryFn: () => fetchAssets({ q: assetSearch }, 0, 10),
    enabled: assetSearch.trim().length > 1,
  })

  function openDialog() {
    setScopeType('ASSET')
    setAssetSearch('')
    setSelectedAssetId('')
    setSelectedAuditId('')
    setReason('')
    setError(null)
    setDialogOpen(true)
  }

  async function handlePlace() {
    setError(null)
    const scopeId = scopeType === 'ASSET' ? selectedAssetId : selectedAuditId
    try {
      await placeHold.mutateAsync({ scopeType, scopeId, reason })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to place legal hold')
    }
  }

  async function handleLift() {
    if (!liftTarget) return
    setError(null)
    try {
      await liftHold.mutateAsync({ id: liftTarget, liftReason })
      setLiftTarget(null)
      setLiftReason('')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to lift legal hold')
    }
  }

  if (holdsQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (holdsQuery.isError) return <ErrorPanel error={holdsQuery.error} onRetry={() => holdsQuery.refetch()} />

  const canSubmit = reason.trim().length > 0 && (scopeType === 'ASSET' ? !!selectedAssetId : !!selectedAuditId)

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="subtitle1">Legal Holds</Typography>
        {canWrite && (
          <Button variant="contained" size="small" onClick={openDialog}>
            Place Hold
          </Button>
        )}
      </Stack>

      {error && !dialogOpen && <Alert severity="error">{error}</Alert>}

      <List dense>
        {(holdsQuery.data ?? []).map((hold) => (
          <ListItem
            key={hold.id}
            divider
            secondaryAction={
              canWrite &&
              hold.active && (
                <Button size="small" color="error" onClick={() => setLiftTarget(hold.id)}>
                  Lift
                </Button>
              )
            }
          >
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Chip size="small" label={hold.scopeType} />
                  <Typography variant="body2">{hold.scopeId}</Typography>
                  <Chip size="small" color={hold.active ? 'warning' : 'default'} label={hold.active ? 'ACTIVE' : 'LIFTED'} />
                </Stack>
              }
              slotProps={{ secondary: { component: 'div' } }}
              secondary={hold.reason}
            />
          </ListItem>
        ))}
        {(holdsQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No legal holds recorded.
          </Typography>
        )}
      </List>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Place Legal Hold</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <FormControl fullWidth size="small">
              <InputLabel id="hold-scope-type-label">Scope Type</InputLabel>
              <Select
                labelId="hold-scope-type-label"
                label="Scope Type"
                value={scopeType}
                onChange={(e) => setScopeType(e.target.value as LegalHoldScopeType)}
              >
                <MenuItem value="ASSET">Asset</MenuItem>
                <MenuItem value="AUDIT">Audit</MenuItem>
              </Select>
            </FormControl>

            {scopeType === 'ASSET' ? (
              <Autocomplete
                options={assetSearchQuery.data?.data ?? []}
                getOptionLabel={(asset) => `${asset.assetNumber} — ${asset.name}`}
                loading={assetSearchQuery.isFetching}
                inputValue={assetSearch}
                onInputChange={(_, value) => setAssetSearch(value)}
                onChange={(_, value) => setSelectedAssetId(value?.id ?? '')}
                renderInput={(params) => <TextField {...params} label="Search asset by number or name" size="small" />}
              />
            ) : (
              <FormControl fullWidth size="small">
                <InputLabel id="hold-audit-label">Audit</InputLabel>
                <Select
                  labelId="hold-audit-label"
                  label="Audit"
                  value={selectedAuditId}
                  onChange={(e) => setSelectedAuditId(e.target.value)}
                >
                  {(auditsQuery.data ?? []).map((audit) => (
                    <MenuItem key={audit.id} value={audit.id}>
                      {audit.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            <TextField
              label="Reason"
              required
              fullWidth
              multiline
              minRows={2}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handlePlace} disabled={!canSubmit || placeHold.isPending}>
            Place Hold
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!liftTarget} onClose={() => setLiftTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Lift Legal Hold</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <TextField
            label="Lift Reason"
            required
            fullWidth
            multiline
            minRows={2}
            sx={{ mt: 1 }}
            value={liftReason}
            onChange={(e) => setLiftReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLiftTarget(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleLift} disabled={!liftReason || liftHold.isPending}>
            Lift Hold
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
