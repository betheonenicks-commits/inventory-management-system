import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useDepreciationOverrideQuery, useDepreciationQuery, useUpsertDepreciationOverrideMutation } from '../hooks/useAssetDepreciation'
import type { Asset, DepreciationMethod } from '../types'

// No fiscal-year configuration exists anywhere in the codebase and isn't
// invented here - NBV is computed as-of today (or a plain date param), not
// against a configured fiscal calendar.
export function AssetDepreciationPanel({ asset }: { asset: Asset }) {
  const depreciationQuery = useDepreciationQuery(asset.id)
  const overrideQuery = useDepreciationOverrideQuery(asset.id)
  const upsert = useUpsertDepreciationOverrideMutation(asset.id)
  const [editing, setEditing] = useState(false)

  const result = depreciationQuery.data
  const override = overrideQuery.data

  const [form, setForm] = useState<{ method: '' | DepreciationMethod; usefulLifeMonths: string; salvageValuePct: string }>({
    method: '',
    usefulLifeMonths: '',
    salvageValuePct: '',
  })

  function startEditing() {
    setForm({
      method: override?.method ?? '',
      usefulLifeMonths: override?.usefulLifeMonths != null ? String(override.usefulLifeMonths) : '',
      salvageValuePct: override?.salvageValuePct != null ? String(override.salvageValuePct) : '',
    })
    setEditing(true)
  }

  async function handleSave() {
    await upsert.mutateAsync({
      method: form.method || undefined,
      usefulLifeMonths: form.usefulLifeMonths ? Number(form.usefulLifeMonths) : undefined,
      salvageValuePct: form.salvageValuePct ? Number(form.salvageValuePct) : undefined,
      version: override?.version,
    })
    setEditing(false)
  }

  if (depreciationQuery.isLoading) {
    return null
  }

  return (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Depreciation
      </Typography>

      {!editing ? (
        <Stack spacing={0.5}>
          {result?.status === 'COMPUTED' ? (
            <>
              <Typography variant="body2">Net Book Value: {result.netBookValue}</Typography>
              <Typography variant="caption" color="text.secondary">
                {result.method} · {result.usefulLifeMonths}mo useful life · accumulated {result.accumulatedDepreciation} as of {result.asOf}
              </Typography>
            </>
          ) : (
            <Typography variant="body2" color="text.secondary">
              Not depreciated — no method/useful life configured (category default or override).
            </Typography>
          )}
          <Button size="small" onClick={startEditing} sx={{ alignSelf: 'flex-start', mt: 0.5 }}>
            {override ? 'Edit Override' : 'Add Override'}
          </Button>
        </Stack>
      ) : (
        <Stack spacing={1}>
          <Grid container spacing={1}>
            <Grid size={12}>
              <TextField
                size="small"
                fullWidth
                select
                label="Method Override"
                value={form.method}
                onChange={(e) => setForm({ ...form, method: e.target.value as '' | DepreciationMethod })}
              >
                <MenuItem value="">Use category default</MenuItem>
                <MenuItem value="STRAIGHT_LINE">Straight Line</MenuItem>
                <MenuItem value="DECLINING_BALANCE">Declining Balance</MenuItem>
              </TextField>
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="number"
                label="Useful Life (months)"
                value={form.usefulLifeMonths}
                onChange={(e) => setForm({ ...form, usefulLifeMonths: e.target.value })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="number"
                label="Salvage Value %"
                value={form.salvageValuePct}
                onChange={(e) => setForm({ ...form, salvageValuePct: e.target.value })}
              />
            </Grid>
          </Grid>
          <Stack direction="row" spacing={1}>
            <Button size="small" variant="contained" disabled={upsert.isPending} onClick={handleSave}>
              Save
            </Button>
            <Button size="small" color="inherit" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </Stack>
        </Stack>
      )}
    </Box>
  )
}
