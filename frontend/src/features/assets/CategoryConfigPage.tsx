import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControlLabel from '@mui/material/FormControlLabel'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../api/errors'
import {
  useAssetCategoriesQuery,
  useCreateAssetCategoryMutation,
  useDeleteAssetCategoryMutation,
} from './hooks/useAssetCategoriesQuery'
import type { CustomFieldDataType } from './types'
import type { CustomFieldDefinitionPayload } from '../../api/assets/assetCategoryApi'

const DATA_TYPES: CustomFieldDataType[] = ['TEXT', 'NUMBER', 'DATE', 'BOOLEAN', 'ENUM']

function emptyField(): CustomFieldDefinitionPayload {
  return { fieldKey: '', label: '', dataType: 'TEXT', required: false }
}

export function CategoryConfigPage() {
  const categoriesQuery = useAssetCategoriesQuery()
  const createCategory = useCreateAssetCategoryMutation()
  const deleteCategory = useDeleteAssetCategoryMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [fields, setFields] = useState<CustomFieldDefinitionPayload[]>([])
  const [error, setError] = useState<string | null>(null)
  const [deleteBlockedMessage, setDeleteBlockedMessage] = useState<string | null>(null)

  function openDialog() {
    setName('')
    setCode('')
    setFields([])
    setError(null)
    setDialogOpen(true)
  }

  function updateField(index: number, patch: Partial<CustomFieldDefinitionPayload>) {
    setFields((prev) => prev.map((f, i) => (i === index ? { ...f, ...patch } : f)))
  }

  async function handleCreate() {
    setError(null)
    try {
      await createCategory.mutateAsync({ name, code, customFields: fields })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save category')
    }
  }

  async function handleDelete(id: string) {
    setDeleteBlockedMessage(null)
    try {
      await deleteCategory.mutateAsync(id)
    } catch (err) {
      if (isApiProblem(err) && err.status === 409) {
        setDeleteBlockedMessage(err.detail)
      }
    }
  }

  return (
    <Box>
      <PageHeader
        title="Asset Categories"
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
            New Category
          </Button>
        }
      />

      {deleteBlockedMessage && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setDeleteBlockedMessage(null)}>
          {deleteBlockedMessage}
        </Alert>
      )}

      {categoriesQuery.isLoading && <LoadingSkeleton rows={4} />}
      {categoriesQuery.isError && <ErrorPanel error={categoriesQuery.error} onRetry={() => categoriesQuery.refetch()} />}

      {categoriesQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {categoriesQuery.data.map((category) => (
              <ListItem
                key={category.id}
                divider
                secondaryAction={
                  <IconButton edge="end" onClick={() => handleDelete(category.id)}>
                    <DeleteIcon />
                  </IconButton>
                }
              >
                <ListItemText
                  primary={category.name}
                  secondary={
                    <Stack direction="row" spacing={1} sx={{ mt: 0.5, flexWrap: 'wrap' }}>
                      <Chip size="small" label={category.code} variant="outlined" />
                      {category.customFields.map((f) => (
                        <Chip key={f.id} size="small" label={`${f.label} (${f.dataType.toLowerCase()})`} />
                      ))}
                    </Stack>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Asset Category</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Name" fullWidth required value={name} onChange={(e) => setName(e.target.value)} />
            <TextField label="Code" fullWidth required value={code} onChange={(e) => setCode(e.target.value)} />

            <Typography variant="subtitle2">Custom Fields</Typography>
            {fields.map((field, index) => (
              <Stack key={index} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                <TextField
                  label="Field Key"
                  size="small"
                  value={field.fieldKey}
                  onChange={(e) => updateField(index, { fieldKey: e.target.value })}
                />
                <TextField
                  label="Label"
                  size="small"
                  value={field.label}
                  onChange={(e) => updateField(index, { label: e.target.value })}
                />
                <TextField
                  select
                  label="Type"
                  size="small"
                  value={field.dataType}
                  onChange={(e) => updateField(index, { dataType: e.target.value as CustomFieldDataType })}
                  sx={{ minWidth: 110 }}
                >
                  {DATA_TYPES.map((t) => (
                    <MenuItem key={t} value={t}>
                      {t}
                    </MenuItem>
                  ))}
                </TextField>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={field.required}
                      onChange={(e) => updateField(index, { required: e.target.checked })}
                    />
                  }
                  label="Required"
                />
                <IconButton onClick={() => setFields((prev) => prev.filter((_, i) => i !== index))}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Stack>
            ))}
            <Button startIcon={<AddIcon />} onClick={() => setFields((prev) => [...prev, emptyField()])}>
              Add Field
            </Button>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!name || !code || createCategory.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
