import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useUpsertVehicleDetailMutation, useVehicleDetailQuery } from '../hooks/useAssetVehicle'
import type { Asset } from '../types'

// Only rendered when the asset's category is flagged as requiring vehicle
// fields (AC-AST-15's "don't clutter unrelated categories") - the caller
// decides that, this component assumes it's already been checked.
export function AssetVehiclePanel({ asset }: { asset: Asset }) {
  const vehicleQuery = useVehicleDetailQuery(asset.id)
  const upsert = useUpsertVehicleDetailMutation(asset.id)
  const [editing, setEditing] = useState(false)

  const detail = vehicleQuery.data

  const [form, setForm] = useState({
    vin: '',
    registrationNumber: '',
    odometerReading: '',
    odometerUnit: 'MI',
    registrationExpiryDate: '',
    insuranceExpiryDate: '',
  })

  function startEditing() {
    setForm({
      vin: detail?.vin ?? '',
      registrationNumber: detail?.registrationNumber ?? '',
      odometerReading: detail?.odometerReading != null ? String(detail.odometerReading) : '',
      odometerUnit: detail?.odometerUnit ?? 'MI',
      registrationExpiryDate: detail?.registrationExpiryDate ?? '',
      insuranceExpiryDate: detail?.insuranceExpiryDate ?? '',
    })
    setEditing(true)
  }

  async function handleSave() {
    await upsert.mutateAsync({
      vin: form.vin || undefined,
      registrationNumber: form.registrationNumber || undefined,
      odometerReading: form.odometerReading ? Number(form.odometerReading) : undefined,
      odometerUnit: form.odometerUnit,
      registrationExpiryDate: form.registrationExpiryDate || undefined,
      insuranceExpiryDate: form.insuranceExpiryDate || undefined,
      version: detail?.version,
    })
    setEditing(false)
  }

  if (vehicleQuery.isLoading) {
    return null
  }

  return (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Vehicle Details
      </Typography>

      {!editing ? (
        <Stack spacing={0.5}>
          {detail ? (
            <>
              <Typography variant="body2">VIN {detail.vin ?? '—'} · Plate {detail.registrationNumber ?? '—'}</Typography>
              <Typography variant="caption" color="text.secondary">
                Odometer {detail.odometerReading ?? '—'} {detail.odometerUnit} · Registration expires {detail.registrationExpiryDate ?? '—'}
              </Typography>
            </>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No vehicle details on file.
            </Typography>
          )}
          <Button size="small" onClick={startEditing} sx={{ alignSelf: 'flex-start', mt: 0.5 }}>
            {detail ? 'Edit' : 'Add details'}
          </Button>
        </Stack>
      ) : (
        <Stack spacing={1}>
          <Grid container spacing={1}>
            <Grid size={6}>
              <TextField size="small" fullWidth label="VIN" value={form.vin} onChange={(e) => setForm({ ...form, vin: e.target.value })} />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                label="Registration Number"
                value={form.registrationNumber}
                onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
              />
            </Grid>
            <Grid size={4}>
              <TextField
                size="small"
                fullWidth
                type="number"
                label="Odometer"
                value={form.odometerReading}
                onChange={(e) => setForm({ ...form, odometerReading: e.target.value })}
              />
            </Grid>
            <Grid size={2}>
              <TextField
                size="small"
                fullWidth
                select
                label="Unit"
                value={form.odometerUnit}
                onChange={(e) => setForm({ ...form, odometerUnit: e.target.value })}
              >
                <MenuItem value="MI">MI</MenuItem>
                <MenuItem value="KM">KM</MenuItem>
              </TextField>
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="date"
                label="Registration Expiry"
                slotProps={{ inputLabel: { shrink: true } }}
                value={form.registrationExpiryDate}
                onChange={(e) => setForm({ ...form, registrationExpiryDate: e.target.value })}
              />
            </Grid>
            <Grid size={6}>
              <TextField
                size="small"
                fullWidth
                type="date"
                label="Insurance Expiry"
                slotProps={{ inputLabel: { shrink: true } }}
                value={form.insuranceExpiryDate}
                onChange={(e) => setForm({ ...form, insuranceExpiryDate: e.target.value })}
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
