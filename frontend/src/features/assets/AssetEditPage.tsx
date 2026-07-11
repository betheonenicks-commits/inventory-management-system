import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../api/errors'
import { ConflictDialog } from './components/ConflictDialog'
import { useAssetQuery } from './hooks/useAssetQuery'
import { useUpdateAssetMutation } from './hooks/useUpdateAssetMutation'
import type { Asset } from './types'

type FormState = {
  name: string
  manufacturer: string
  model: string
  serialNumber: string
  vendorName: string
  purchaseOrderReference: string
  purchaseCost: string
  warrantyEndDate: string
}

function toFormState(asset: Asset): FormState {
  return {
    name: asset.name,
    manufacturer: asset.manufacturer ?? '',
    model: asset.model ?? '',
    serialNumber: asset.serialNumber ?? '',
    vendorName: asset.vendorName ?? '',
    purchaseOrderReference: asset.purchaseOrderReference ?? '',
    purchaseCost: asset.purchaseCost != null ? String(asset.purchaseCost) : '',
    warrantyEndDate: asset.warrantyEndDate ?? '',
  }
}

export function AssetEditPage() {
  const { assetId } = useParams<{ assetId: string }>()
  const navigate = useNavigate()
  const assetQuery = useAssetQuery(assetId)
  const updateAsset = useUpdateAssetMutation(assetId ?? '')

  const [form, setForm] = useState<FormState | null>(null);
  const [conflict, setConflict] = useState<{ server: Asset } | null>(null)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (assetQuery.data && form === null) {
      setForm(toFormState(assetQuery.data))
    }
  }, [assetQuery.data, form])

  if (assetQuery.isLoading || !form) {
    return <LoadingSkeleton rows={6} />
  }
  if (assetQuery.isError) {
    return <ErrorPanel error={assetQuery.error} onRetry={() => assetQuery.refetch()} />
  }
  const asset = assetQuery.data!

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form) return
    setSubmitError(null)
    setFieldErrors({})
    try {
      await updateAsset.mutateAsync({
        version: asset.version,
        name: form.name,
        manufacturer: form.manufacturer || undefined,
        model: form.model || undefined,
        serialNumber: form.serialNumber || undefined,
        vendorName: form.vendorName || undefined,
        purchaseOrderReference: form.purchaseOrderReference || undefined,
        purchaseCost: form.purchaseCost ? Number(form.purchaseCost) : undefined,
        warrantyEndDate: form.warrantyEndDate || undefined,
      })
      navigate(`/assets/${asset.id}`)
    } catch (err) {
      if (isApiProblem(err) && err.errorCode === 'OPTIMISTIC_LOCK_CONFLICT') {
        setConflict({ server: err.currentResource as Asset })
        return
      }
      if (isApiProblem(err)) {
        if (err.errors) {
          const errors: Record<string, string> = {}
          for (const e2 of err.errors) errors[e2.field] = e2.message
          setFieldErrors(errors)
        }
        setSubmitError(err.detail)
      } else {
        setSubmitError('Something went wrong while saving this asset.')
      }
    }
  }

  function handleReload() {
    setConflict(null)
    assetQuery.refetch().then((result) => {
      if (result.data) setForm(toFormState(result.data))
    })
  }

  return (
    <Box sx={{ maxWidth: 800 }}>
      <PageHeader title={`Edit ${asset.assetNumber}`} />
      <Paper variant="outlined" sx={{ p: 3 }} component="form" onSubmit={handleSubmit}>
        {submitError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {submitError}
          </Alert>
        )}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              required
              label="Name"
              value={form.name}
              error={!!fieldErrors.name}
              helperText={fieldErrors.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              label="Manufacturer"
              value={form.manufacturer}
              onChange={(e) => setForm({ ...form, manufacturer: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              label="Model"
              value={form.model}
              onChange={(e) => setForm({ ...form, model: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              label="Serial Number"
              value={form.serialNumber}
              onChange={(e) => setForm({ ...form, serialNumber: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              label="Vendor"
              value={form.vendorName}
              onChange={(e) => setForm({ ...form, vendorName: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              label="Purchase Order Reference"
              value={form.purchaseOrderReference}
              onChange={(e) => setForm({ ...form, purchaseOrderReference: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              type="number"
              label="Purchase Cost"
              error={!!fieldErrors.purchaseCost}
              helperText={fieldErrors.purchaseCost}
              value={form.purchaseCost}
              onChange={(e) => setForm({ ...form, purchaseCost: e.target.value })}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              fullWidth
              type="date"
              label="Warranty End"
              slotProps={{ inputLabel: { shrink: true } }}
              value={form.warrantyEndDate}
              onChange={(e) => setForm({ ...form, warrantyEndDate: e.target.value })}
            />
          </Grid>
        </Grid>

        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 3 }}>
          <Button onClick={() => navigate(`/assets/${asset.id}`)}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={updateAsset.isPending}>
            {updateAsset.isPending ? 'Saving...' : 'Save Changes'}
          </Button>
        </Box>
      </Paper>

      <ConflictDialog
        open={!!conflict}
        localValues={{ ...asset, ...form, purchaseCost: form.purchaseCost ? Number(form.purchaseCost) : null }}
        serverResource={conflict?.server ?? null}
        onReload={handleReload}
        onCancel={() => setConflict(null)}
      />
    </Box>
  )
}
