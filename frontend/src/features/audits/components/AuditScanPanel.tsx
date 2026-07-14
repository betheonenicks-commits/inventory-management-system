import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../../api/errors'
import type { Asset } from '../../assets/types'
import { useAssetSearchQuery } from '../hooks/useAssetSearchQuery'
import { useRecordScanMutation } from '../hooks/useAuditScanMutation'
import type { AssetCondition } from '../types'

const CONDITIONS: AssetCondition[] = ['GOOD', 'FAIR', 'MINOR_DAMAGE', 'MAJOR_DAMAGE', 'UNUSABLE']

/**
 * US-AUD-05/06/10/12: verify a scanned asset with condition + remarks. This
 * is a form-based single-scan entry, not literal barcode/continuous-scan
 * hardware integration (US-AUD-06's "no extra navigation step" is satisfied
 * here by the form staying open and clearing itself after each successful
 * scan, ready for the next one - not by driving real scanner hardware, which
 * this browser-only frontend has no access to).
 */
export function AuditScanPanel({ auditId }: { auditId: string }) {
  const [searchText, setSearchText] = useState('')
  const [selectedAsset, setSelectedAsset] = useState<Asset | null>(null)
  const [condition, setCondition] = useState<AssetCondition>('GOOD')
  const [remarks, setRemarks] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [lastResult, setLastResult] = useState<string | null>(null)

  const searchQuery = useAssetSearchQuery(searchText)
  const recordScan = useRecordScanMutation(auditId)

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
      setSelectedAsset(null)
      setSearchText('')
      setRemarks('')
      setCondition('GOOD')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to record scan')
    }
  }

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Scan an Asset</Typography>

      {error && <Alert severity="error">{error}</Alert>}
      {lastResult && (
        <Alert severity="success" onClose={() => setLastResult(null)}>
          {lastResult}
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

      <Box>
        <Button variant="contained" onClick={handleRecordScan} disabled={!selectedAsset || recordScan.isPending}>
          Record Scan
        </Button>
      </Box>
    </Stack>
  )
}
