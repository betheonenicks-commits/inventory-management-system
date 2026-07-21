import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import DeleteIcon from '@mui/icons-material/Delete'
import IconButton from '@mui/material/IconButton'
import { isApiProblem } from '../../../api/errors'
import type { Asset } from '../../assets/types'
import { useAssetSearchQuery } from '../hooks/useAssetSearchQuery'
import { useRecordBatchScanMutation, useRecordScanMutation } from '../hooks/useAuditScanMutation'
import type { AuditBatchScanResult } from '../../../api/audits/auditApi'
import type { AssetCondition } from '../types'

const CONDITIONS: AssetCondition[] = ['GOOD', 'FAIR', 'MINOR_DAMAGE', 'MAJOR_DAMAGE', 'UNUSABLE']

interface PendingScan {
  asset: Asset
  condition: AssetCondition
  remarks: string
}

/**
 * US-AUD-05/06/07/10/12: verify a scanned asset with condition + remarks, one
 * at a time or (the toggle this session added) as a batch. Single-scan mode
 * IS continuous scan mode (labelled explicitly now, not just an implicit
 * default): AC-AUD-06-H's "no extra navigation step" is satisfied by the form
 * staying open and clearing itself after each successful scan, ready for the
 * next one - not by driving real scanner hardware, which this browser-only
 * frontend has no access to. AC-AUD-06-X ("exit continuous mode to record a
 * finding, place in the sequence preserved on return") doesn't map cleanly
 * onto this system's scan-by-search design - there is no fixed ordered scan
 * queue to have a "place" in, since each scan independently searches for any
 * asset rather than walking a pre-built list - so that clause is not claimed
 * satisfied; recording a finding (via the Exceptions panel) doesn't navigate
 * away from this form at all, so there is nothing to lose regardless. Batch
 * mode builds a local pending list, one asset+condition+remarks entry at a
 * time, then submits it all in a single request to `/scans/batch` and shows
 * the verified/duplicate/unrecognized breakdown the endpoint returns.
 */
export function AuditScanPanel({ auditId }: { auditId: string }) {
  const [batchMode, setBatchMode] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [selectedAsset, setSelectedAsset] = useState<Asset | null>(null)
  const [condition, setCondition] = useState<AssetCondition>('GOOD')
  const [remarks, setRemarks] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [lastResult, setLastResult] = useState<string | null>(null)
  const [pending, setPending] = useState<PendingScan[]>([])
  const [batchResult, setBatchResult] = useState<AuditBatchScanResult | null>(null)

  const searchQuery = useAssetSearchQuery(searchText)
  const recordScan = useRecordScanMutation(auditId)
  const recordBatchScan = useRecordBatchScanMutation(auditId)

  function resetEntryFields() {
    setSelectedAsset(null)
    setSearchText('')
    setRemarks('')
    setCondition('GOOD')
  }

  async function handleRecordScan() {
    if (!selectedAsset) return
    setError(null)
    setLastResult(null)
    try {
      const finding = await recordScan.mutateAsync({
        assetId: selectedAsset.id,
        condition,
        remarks: remarks || undefined,
      })
      setLastResult(`${selectedAsset.assetNumber} recorded as ${finding.status}`)
      // Ready for the next scan immediately (US-AUD-06) - no extra navigation step.
      resetEntryFields()
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to record scan')
    }
  }

  function handleAddToBatch() {
    if (!selectedAsset) return
    setPending((p) => [...p, { asset: selectedAsset, condition, remarks }])
    resetEntryFields()
  }

  function handleRemoveFromBatch(index: number) {
    setPending((p) => p.filter((_, i) => i !== index))
  }

  async function handleSubmitBatch() {
    if (pending.length === 0) return
    setError(null)
    setBatchResult(null)
    try {
      const result = await recordBatchScan.mutateAsync(
        pending.map((p) => ({ assetId: p.asset.id, condition: p.condition, remarks: p.remarks || undefined })),
      )
      setBatchResult(result)
      setPending([])
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to submit batch')
    }
  }

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <Typography variant="subtitle1">Scan an Asset</Typography>
          {!batchMode && (
            <Chip
              size="small"
              variant="outlined"
              color="primary"
              label="Continuous scan mode"
              title="Each successful scan immediately readies the input for the next asset - no extra navigation step (US-AUD-06)."
            />
          )}
        </Stack>
        <FormControlLabel
          control={<Switch size="small" checked={batchMode} onChange={(e) => setBatchMode(e.target.checked)} />}
          label="Batch mode"
        />
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {lastResult && (
        <Alert severity="success" onClose={() => setLastResult(null)}>
          {lastResult}
        </Alert>
      )}
      {batchResult && (
        <Alert severity="success" onClose={() => setBatchResult(null)}>
          Batch submitted: {batchResult.summary.verifiedCount} verified, {batchResult.summary.outOfScopeCount} out of
          scope, {batchResult.summary.duplicateCount} duplicate, {batchResult.summary.unrecognizedCount} unrecognized.
        </Alert>
      )}

      <Autocomplete
        options={searchQuery.data?.data ?? []}
        getOptionLabel={(asset) => `${asset.assetNumber} — ${asset.name}`}
        loading={searchQuery.isFetching}
        value={selectedAsset}
        onChange={(_, value) => setSelectedAsset(value)}
        inputValue={searchText}
        onInputChange={(_, value) => setSearchText(value)}
        isOptionEqualToValue={(option, value) => option.id === value.id}
        renderInput={(params) => <TextField {...params} label="Search by asset number or name" size="small" />}
      />

      <FormControl size="small" fullWidth>
        <InputLabel id="condition-label">Condition</InputLabel>
        <Select
          labelId="condition-label"
          label="Condition"
          value={condition}
          onChange={(e) => setCondition(e.target.value as AssetCondition)}
        >
          {CONDITIONS.map((c) => (
            <MenuItem key={c} value={c}>
              {c.replace('_', ' ')}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <TextField
        label="Remarks"
        size="small"
        multiline
        minRows={2}
        value={remarks}
        onChange={(e) => setRemarks(e.target.value)}
      />

      {batchMode ? (
        <>
          <Box>
            <Button variant="outlined" onClick={handleAddToBatch} disabled={!selectedAsset}>
              Add to Batch
            </Button>
          </Box>
          {pending.length > 0 && (
            <List dense sx={{ bgcolor: 'action.hover', borderRadius: 1 }}>
              {pending.map((p, i) => (
                <ListItem
                  key={`${p.asset.id}-${i}`}
                  secondaryAction={
                    <IconButton edge="end" size="small" onClick={() => handleRemoveFromBatch(i)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  }
                >
                  <ListItemText
                    primary={`${p.asset.assetNumber} — ${p.asset.name}`}
                    secondary={
                      <Chip size="small" variant="outlined" label={p.condition.replace('_', ' ')} sx={{ mt: 0.5 }} />
                    }
                    slotProps={{ secondary: { component: 'div' } }}
                  />
                </ListItem>
              ))}
            </List>
          )}
          <Box>
            <Button
              variant="contained"
              onClick={handleSubmitBatch}
              disabled={pending.length === 0 || recordBatchScan.isPending}
            >
              Submit Batch ({pending.length})
            </Button>
          </Box>
        </>
      ) : (
        <Box>
          <Button variant="contained" onClick={handleRecordScan} disabled={!selectedAsset || recordScan.isPending}>
            Record Scan
          </Button>
        </Box>
      )}
    </Stack>
  )
}
