import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
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
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import {
  useCreateDepartmentMutation,
  useDeleteDepartmentMutation,
  useDepartmentsAdminQuery,
  useUpdateDepartmentMutation,
} from './hooks/useDepartmentsQuery'
import type { Department } from '../../api/org/departmentApi'

/** US-ORG-03: department/cost-center CRUD - previously fully built with no admin UI to reach it. */
export function DepartmentListPage() {
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'org:write')
  const departmentsQuery = useDepartmentsAdminQuery()
  const createDepartment = useCreateDepartmentMutation()
  const updateDepartment = useUpdateDepartmentMutation()
  const deleteDepartment = useDeleteDepartmentMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Department | null>(null)
  const [name, setName] = useState('')
  const [costCenterCode, setCostCenterCode] = useState('')
  const [active, setActive] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deleteBlockedMessage, setDeleteBlockedMessage] = useState<string | null>(null)

  function openCreateDialog() {
    setEditing(null)
    setName('')
    setCostCenterCode('')
    setActive(true)
    setError(null)
    setDialogOpen(true)
  }

  function openEditDialog(department: Department) {
    setEditing(department)
    setName(department.name)
    setCostCenterCode(department.costCenterCode)
    setActive(department.active)
    setError(null)
    setDialogOpen(true)
  }

  async function handleSave() {
    setError(null)
    try {
      if (editing) {
        await updateDepartment.mutateAsync({
          id: editing.id,
          payload: { name, costCenterCode, active, version: editing.version },
        })
      } else {
        await createDepartment.mutateAsync({ name, costCenterCode })
      }
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save department')
    }
  }

  async function handleDelete(department: Department) {
    setDeleteBlockedMessage(null)
    try {
      await deleteDepartment.mutateAsync(department.id)
    } catch (err) {
      if (isApiProblem(err) && err.status === 409) {
        setDeleteBlockedMessage(err.detail)
      }
    }
  }

  const saving = createDepartment.isPending || updateDepartment.isPending

  return (
    <Box>
      <PageHeader
        title="Departments"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
              New Department
            </Button>
          )
        }
      />

      {deleteBlockedMessage && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setDeleteBlockedMessage(null)}>
          {deleteBlockedMessage}
        </Alert>
      )}

      {departmentsQuery.isLoading && <LoadingSkeleton rows={4} />}
      {departmentsQuery.isError && (
        <ErrorPanel error={departmentsQuery.error} onRetry={() => departmentsQuery.refetch()} />
      )}

      {departmentsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {departmentsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <ListItemText primary="No departments yet." />
              </Box>
            )}
            {departmentsQuery.data.map((department) => (
              <ListItem
                key={department.id}
                divider
                secondaryAction={
                  canWrite && (
                    <Stack direction="row" spacing={0.5}>
                      <IconButton edge="end" size="small" onClick={() => openEditDialog(department)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton edge="end" size="small" onClick={() => handleDelete(department)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  )
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <span>{department.name}</span>
                      <Chip size="small" variant="outlined" label={department.costCenterCode} />
                      {!department.active && <Chip size="small" color="default" label="Inactive" />}
                    </Stack>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Edit Department' : 'New Department'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Name" fullWidth required value={name} onChange={(e) => setName(e.target.value)} />
            <TextField
              label="Cost Center Code"
              fullWidth
              required
              value={costCenterCode}
              onChange={(e) => setCostCenterCode(e.target.value)}
            />
            {editing && (
              <FormControlLabel
                control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />}
                label="Active"
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={!name.trim() || !costCenterCode.trim() || saving}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
