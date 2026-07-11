import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Step from '@mui/material/Step'
import StepLabel from '@mui/material/StepLabel'
import Stepper from '@mui/material/Stepper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { isApiProblem } from '../../api/errors'
import { DynamicCustomFieldsForm } from './components/DynamicCustomFieldsForm'
import { useAssetCategoriesQuery } from './hooks/useAssetCategoriesQuery'
import { useCreateAssetMutation } from './hooks/useCreateAssetMutation'
import { assetCoreSchema } from './schemas/assetSchemas'

const STEPS = ['Category', 'Details', 'Review']

export function AssetRegisterWizardPage() {
  const navigate = useNavigate()
  const categoriesQuery = useAssetCategoriesQuery()
  const createAsset = useCreateAssetMutation()

  const [activeStep, setActiveStep] = useState(0)
  const [categoryId, setCategoryId] = useState('')
  const [coreValues, setCoreValues] = useState({
    name: '',
    manufacturer: '',
    model: '',
    serialNumber: '',
    vendorName: '',
    purchaseOrderReference: '',
    purchaseDate: '',
    purchaseCost: '',
    warrantyStartDate: '',
    warrantyEndDate: '',
  })
  const [customFieldValues, setCustomFieldValues] = useState<Record<string, unknown>>({})
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  const selectedCategory = categoriesQuery.data?.find((c) => c.id === categoryId)

  function validateDetailsStep(): boolean {
    const parsed = assetCoreSchema.safeParse({ categoryId, ...coreValues })
    const errors: Record<string, string> = {}
    if (!parsed.success) {
      for (const issue of parsed.error.issues) {
        errors[String(issue.path[0])] = issue.message
      }
    }
    if (selectedCategory) {
      for (const def of selectedCategory.customFields) {
        const value = customFieldValues[def.fieldKey]
        const blank = value === undefined || value === null || value === ''
        if (def.required && blank) {
          errors[def.fieldKey] = 'This field is required'
        }
      }
    }
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  function handleNext() {
    if (activeStep === 0 && !categoryId) {
      setFieldErrors({ categoryId: 'Please select a category' })
      return
    }
    if (activeStep === 1 && !validateDetailsStep()) {
      return
    }
    setFieldErrors({})
    setActiveStep((s) => s + 1)
  }

  async function handleSubmit() {
    setSubmitError(null)
    try {
      const asset = await createAsset.mutateAsync({
        categoryId,
        name: coreValues.name,
        manufacturer: coreValues.manufacturer || undefined,
        model: coreValues.model || undefined,
        serialNumber: coreValues.serialNumber || undefined,
        vendorName: coreValues.vendorName || undefined,
        purchaseOrderReference: coreValues.purchaseOrderReference || undefined,
        purchaseDate: coreValues.purchaseDate || undefined,
        purchaseCost: coreValues.purchaseCost ? Number(coreValues.purchaseCost) : undefined,
        warrantyStartDate: coreValues.warrantyStartDate || undefined,
        warrantyEndDate: coreValues.warrantyEndDate || undefined,
        customFields: customFieldValues,
      })
      navigate(`/assets/${asset.id}`)
    } catch (err) {
      if (isApiProblem(err)) {
        if (err.errors) {
          const errors: Record<string, string> = {}
          for (const e of err.errors) {
            const key = e.field.startsWith('customFields.') ? e.field.replace('customFields.', '') : e.field
            errors[key] = e.message
          }
          setFieldErrors(errors)
          setActiveStep(1)
        }
        setSubmitError(err.detail)
      } else {
        setSubmitError('Something went wrong while registering this asset.')
      }
    }
  }

  return (
    <Box sx={{ maxWidth: 800 }}>
      <PageHeader title="Register New Asset" />
      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {STEPS.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      <Paper variant="outlined" sx={{ p: 3 }}>
        {submitError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {submitError}
          </Alert>
        )}

        {activeStep === 0 && (
          <TextField
            select
            fullWidth
            label="Category"
            value={categoryId}
            error={!!fieldErrors.categoryId}
            helperText={fieldErrors.categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            {(categoriesQuery.data ?? []).map((c) => (
              <MenuItem key={c.id} value={c.id}>
                {c.name}
              </MenuItem>
            ))}
          </TextField>
        )}

        {activeStep === 1 && (
          <Box>
            <Grid container spacing={2} sx={{ mb: 2 }}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  required
                  label="Name"
                  value={coreValues.name}
                  error={!!fieldErrors.name}
                  helperText={fieldErrors.name}
                  onChange={(e) => setCoreValues((v) => ({ ...v, name: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Manufacturer"
                  value={coreValues.manufacturer}
                  onChange={(e) => setCoreValues((v) => ({ ...v, manufacturer: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Model"
                  value={coreValues.model}
                  onChange={(e) => setCoreValues((v) => ({ ...v, model: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Serial Number"
                  value={coreValues.serialNumber}
                  onChange={(e) => setCoreValues((v) => ({ ...v, serialNumber: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Vendor"
                  value={coreValues.vendorName}
                  onChange={(e) => setCoreValues((v) => ({ ...v, vendorName: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Purchase Order Reference"
                  value={coreValues.purchaseOrderReference}
                  onChange={(e) => setCoreValues((v) => ({ ...v, purchaseOrderReference: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="date"
                  label="Purchase Date"
                  slotProps={{ inputLabel: { shrink: true } }}
                  value={coreValues.purchaseDate}
                  onChange={(e) => setCoreValues((v) => ({ ...v, purchaseDate: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Purchase Cost"
                  error={!!fieldErrors.purchaseCost}
                  helperText={fieldErrors.purchaseCost}
                  value={coreValues.purchaseCost}
                  onChange={(e) => setCoreValues((v) => ({ ...v, purchaseCost: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="date"
                  label="Warranty Start"
                  slotProps={{ inputLabel: { shrink: true } }}
                  value={coreValues.warrantyStartDate}
                  onChange={(e) => setCoreValues((v) => ({ ...v, warrantyStartDate: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="date"
                  label="Warranty End"
                  error={!!fieldErrors.warrantyEndDate}
                  helperText={fieldErrors.warrantyEndDate}
                  slotProps={{ inputLabel: { shrink: true } }}
                  value={coreValues.warrantyEndDate}
                  onChange={(e) => setCoreValues((v) => ({ ...v, warrantyEndDate: e.target.value }))}
                />
              </Grid>
            </Grid>

            {selectedCategory && selectedCategory.customFields.length > 0 && (
              <>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  {selectedCategory.name} Fields
                </Typography>
                <DynamicCustomFieldsForm
                  definitions={selectedCategory.customFields}
                  values={customFieldValues}
                  errors={fieldErrors}
                  onChange={(key, value) => setCustomFieldValues((v) => ({ ...v, [key]: value }))}
                />
              </>
            )}
          </Box>
        )}

        {activeStep === 2 && (
          <Box>
            <Typography variant="body2">
              <strong>Category:</strong> {selectedCategory?.name}
            </Typography>
            <Typography variant="body2">
              <strong>Name:</strong> {coreValues.name}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              A unique asset number and printable label will be generated automatically once submitted.
            </Typography>
          </Box>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
          <Button disabled={activeStep === 0} onClick={() => setActiveStep((s) => s - 1)}>
            Back
          </Button>
          {activeStep < STEPS.length - 1 ? (
            <Button variant="contained" onClick={handleNext}>
              Next
            </Button>
          ) : (
            <Button variant="contained" onClick={handleSubmit} disabled={createAsset.isPending}>
              {createAsset.isPending ? 'Registering...' : 'Register Asset'}
            </Button>
          )}
        </Box>
      </Paper>
    </Box>
  )
}
