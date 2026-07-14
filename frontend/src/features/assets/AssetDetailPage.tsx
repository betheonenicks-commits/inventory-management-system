import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import EditIcon from '@mui/icons-material/Edit'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { StatusChip } from './components/StatusChip'
import { AssetHistoryTimeline } from './components/AssetHistoryTimeline'
import { AssetChildrenPanel } from './components/AssetChildrenPanel'
import { AssetAssignmentPanel } from './components/AssetAssignmentPanel'
import { AssetInsurancePanel } from './components/AssetInsurancePanel'
import { AssetVehiclePanel } from './components/AssetVehiclePanel'
import { AssetDepreciationPanel } from './components/AssetDepreciationPanel'
import { LabelPreview } from './components/LabelPreview'
import { useAssetQuery, useAssetHistoryQuery, useAssetMovementsQuery } from './hooks/useAssetQuery'
import { useChangeAssetStatusMutation } from './hooks/useUpdateAssetMutation'
import { useAssetStatusesQuery } from './hooks/useAssetStatusesQuery'
import { useAuthStore, hasPermission } from '../../auth/authStore'

function Field({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <Box sx={{ mb: 1.5 }}>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
        {label}
      </Typography>
      <Typography variant="body2">{value ?? '—'}</Typography>
    </Box>
  )
}

export function AssetDetailPage() {
  const { assetId } = useParams<{ assetId: string }>()
  const navigate = useNavigate()
  const assetQuery = useAssetQuery(assetId)
  const historyQuery = useAssetHistoryQuery(assetId)
  const movementsQuery = useAssetMovementsQuery(assetId)
  const statusesQuery = useAssetStatusesQuery()
  const changeStatus = useChangeAssetStatusMutation(assetId ?? '')
  const [statusMenuOpen, setStatusMenuOpen] = useState(false)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')

  if (assetQuery.isLoading) {
    return <LoadingSkeleton rows={6} />
  }
  if (assetQuery.isError) {
    return <ErrorPanel error={assetQuery.error} onRetry={() => assetQuery.refetch()} />
  }
  const asset = assetQuery.data!

  return (
    <Box>
      <PageHeader
        title={`${asset.name} · ${asset.assetNumber}`}
        actions={
          canWrite && (
            <Button variant="outlined" startIcon={<EditIcon />} onClick={() => navigate(`/assets/${asset.id}/edit`)}>
              Edit
            </Button>
          )
        }
      />

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mb: 2 }}>
              <StatusChip status={asset.status} />
              {canWrite && (
                <Select
                  size="small"
                  open={statusMenuOpen}
                  onOpen={() => setStatusMenuOpen(true)}
                  onClose={() => setStatusMenuOpen(false)}
                  value=""
                  displayEmpty
                  renderValue={() => 'Change status'}
                  onChange={(e) => {
                    changeStatus.mutate({ statusId: e.target.value as string, version: asset.version })
                  }}
                >
                  {(statusesQuery.data ?? [])
                    .filter((s) => s.id !== asset.status.id)
                    .map((s) => (
                      <MenuItem key={s.id} value={s.id}>
                        {s.label}
                      </MenuItem>
                    ))}
                </Select>
              )}
            </Stack>

            <Grid container spacing={2}>
              <Grid size={6}>
                <Field label="Category" value={asset.categoryName} />
                <Field label="Manufacturer" value={asset.manufacturer} />
                <Field label="Model" value={asset.model} />
                <Field label="Serial Number" value={asset.serialNumber} />
                <Field label="Location" value={asset.orgNodeName} />
              </Grid>
              <Grid size={6}>
                <Field label="Vendor" value={asset.vendorName} />
                <Field label="Purchase Order" value={asset.purchaseOrderReference} />
                <Field label="Purchase Date" value={asset.purchaseDate} />
                <Field label="Purchase Cost" value={asset.purchaseCost} />
                <Field label="Warranty Expiry" value={asset.warrantyEndDate} />
                <Field label="RFID Tag ID" value={asset.rfidTagId} />
              </Grid>
            </Grid>

            {Object.keys(asset.customFields).length > 0 && (
              <>
                <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
                  Custom Fields
                </Typography>
                <Grid container spacing={2}>
                  {Object.entries(asset.customFields).map(([key, value]) => (
                    <Grid size={6} key={key}>
                      <Field label={key} value={String(value)} />
                    </Grid>
                  ))}
                </Grid>
              </>
            )}
          </Paper>

          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              History
            </Typography>
            <AssetHistoryTimeline events={historyQuery.data?.data ?? []} />
          </Paper>

          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Movements
            </Typography>
            <AssetHistoryTimeline events={movementsQuery.data?.data ?? []} />
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Label
            </Typography>
            <LabelPreview assetId={asset.id} assetNumber={asset.assetNumber} />
          </Paper>

          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <AssetAssignmentPanel asset={asset} />
          </Paper>

          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <AssetChildrenPanel asset={asset} />
          </Paper>

          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <AssetInsurancePanel asset={asset} />
          </Paper>

          {asset.categoryRequiresVehicleFields && (
            <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
              <AssetVehiclePanel asset={asset} />
            </Paper>
          )}

          <Paper variant="outlined" sx={{ p: 2 }}>
            <AssetDepreciationPanel asset={asset} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
