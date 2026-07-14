import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Grid from '@mui/material/Grid'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useAssetInsuranceQuery, useUpsertAssetInsuranceMutation } from '../hooks/useAssetInsurance'
import type { Asset } from '../types'

// No insurance-expiry report here - that's EPIC-RPT, same accepted gap as
// warranty's report. This panel just captures/edits the current policy.
export function AssetInsurancePanel({ asset }: { asset: Asset }) {
  const insuranceQuery = useAssetInsuranceQuery(asset.id)
  const upsert = useUpsertAssetInsuranceMutation(asset.id)
  const [editing, setEditing] = useState(false)

  const detail = insuranceQuery.data

  const [form, setForm] = useState({
    insurerName: '',
    policyNumber: '',
    coverageAmount: '',
    coverageCurrency: '',
    policyStartDate: '',
    policyExpiryDate: '',
  })

  function startEditing() {
    setForm({
      insurerName: detail?.insurerName ?? '',
      policyNumber: detail?.policyNumber ?? '',
      coverageAmount: detail?.coverageAmount != null ? String(detail.coverageAmount) : '',
      coverageCurrency: detail?.coverageCurrency ?? '',
      policyStartDate: detail?.policyStartDate ?? '',
      policyExpiryDate: detail?.policyExpiryDate ?? '',
    })
    setEditing(true)
  }

  async function handleSave() {
    await upsert.mutateAsync({
      insurerName: form.insurerName || undefined,
      policyNumber: form.policyNumber || undefined,
      coverageAmount: form.coverageAmount ? Number(form.coverageAmount) : undefined,
      coverageCurrency: form.coverageCurrency || undefined,
      policyStartDate: form.policyStartDate || undefined,
      policyExpiryDate: form.policyExpiryDate || undefined,
      version: detail?.version,
    })
    setEditing(false)
  }

  if (insuranceQuery.isLoading) {
    return null
  }

  return (
    <Box>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
        <Typography variant="subtitle1">Insurance</Typography>
        {detail?.expired && <Chip size="small" color="error" label="Expired" />}
      </Stack>

      {!editing ? (
        <Stack spacing={0.5}>
          {detail ? (
            <>
              <Typography variant="body2">{detail.insurerName ?? '—'} · {detail.policyNumber ?? '—'}</Typography>
              <Typography variant="caption" color="text.secondary">
                {detail.coverageAmount != null ? `${detail.coverageCurrency ?? ''} ${detail.coverageAmount}` : 'No coverage amount'} · expires {detail.policyExpiryDate ?? '—'}
              </Typography>
            </>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No policy on file.
            </Typography>
          )}
          <Button size="small" onClick={startEditing} sx={{ alignSelf: 'flex-start', mt: 0.5 }}>
            {detail ? 'Edit' : 'Add policy'}
          </Button>
        </Stack>
      ) : (
        <Stack spacing={1}>
          <Grid container spacing={1}>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                label="Insurer"
                value={form.insurerName}
                onChange={(e) => setForm({ ...form, insurerName: e.target.value })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                label="Policy Number"
                value={form.policyNumber}
                onChange={(e) => setForm({ ...form, policyNumber: e.target.value })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="number"
                label="Coverage Amount"
                value={form.coverageAmount}
                onChange={(e) => setForm({ ...form, coverageAmount: e.target.value })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                label="Currency"
                placeholder="USD"
                value={form.coverageCurrency}
                onChange={(e) => setForm({ ...form, coverageCurrency: e.target.value.toUpperCase() })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="date"
                label="Start Date"
                slotProps={{ inputLabel: { shrink: true } }}
                value={form.policyStartDate}
                onChange={(e) => setForm({ ...form, policyStartDate: e.target.value })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="date"
                label="Expiry Date"
                slotProps={{ inputLabel: { shrink: true } }}
                value={form.policyExpiryDate}
                onChange={(e) => setForm({ ...form, policyExpiryDate: e.target.value })}
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
