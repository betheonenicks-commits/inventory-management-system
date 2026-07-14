import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import LockIcon from '@mui/icons-material/Lock'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../api/errors'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import {
  useCreateRoleMutation,
  useDeleteRoleMutation,
  useRolesQuery,
  useUpdateRoleMutation,
} from './hooks/useRolesQuery'
import type { Role } from './types'

interface RoleFormState {
  code: string
  name: string
  description: string
  permissions: string
}

const EMPTY_FORM: RoleFormState = { code: '', name: '', description: '', permissions: '' }

function toFormState(role: Role): RoleFormState {
  return { code: role.code, name: role.name, description: role.description ?? '', permissions: role.permissions.join(', ') }
}

function parsePermissions(value: string): string[] {
  return value
    .split(',')
    .map((p) => p.trim())
    .filter((p) => p.length > 0)
}

// FR-USR-02: custom roles with a configurable permission set. System roles (the
// FR-USR-01 defaults, the two system-provided custom roles, and Integration
// Service) are shown but never editable/deletable here - the backend enforces
// this too (RoleService.rejectIfSystem), this is just not offering a control
// that would always be refused.
export function RoleListPage() {
  const rolesQuery = useRolesQuery()
  const createRole = useCreateRoleMutation()
  const deleteRole = useDeleteRoleMutation()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'roles:write')

  const [editing, setEditing] = useState<Role | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<RoleFormState>(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)
  const [blockedMessage, setBlockedMessage] = useState<string | null>(null)

  const updateRole = useUpdateRoleMutation(editing?.id ?? '')

  function openCreateDialog() {
    setEditing(null)
    setForm(EMPTY_FORM)
    setError(null)
    setDialogOpen(true)
  }

  function openEditDialog(role: Role) {
    setEditing(role)
    setForm(toFormState(role))
    setError(null)
    setDialogOpen(true)
  }

  async function handleSave() {
    setError(null)
    try {
      if (editing) {
        await updateRole.mutateAsync({
          name: form.name,
          description: form.description || undefined,
          permissions: parsePermissions(form.permissions),
          version: editing.version,
        })
      } else {
        await createRole.mutateAsync({
          code: form.code,
          name: form.name,
          description: form.description || undefined,
          permissions: parsePermissions(form.permissions),
        })
      }
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save role')
    }
  }

  async function handleDelete(id: string) {
    setBlockedMessage(null)
    try {
      await deleteRole.mutateAsync(id)
    } catch (err) {
      if (isApiProblem(err) && err.status === 409) {
        setBlockedMessage(err.detail)
      }
    }
  }

  return (
    <Box>
      <PageHeader
        title="Roles"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
              New Role
            </Button>
          )
        }
      />

      {blockedMessage && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setBlockedMessage(null)}>
          {blockedMessage}
        </Alert>
      )}

      {rolesQuery.isLoading && <LoadingSkeleton rows={6} />}
      {rolesQuery.isError && <ErrorPanel error={rolesQuery.error} onRetry={() => rolesQuery.refetch()} />}

      {rolesQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {rolesQuery.data.map((role) => (
              <ListItem
                key={role.id}
                divider
                secondaryAction={
                  canWrite &&
                  !role.system && (
                    <Stack direction="row" spacing={0.5}>
                      <IconButton edge="end" onClick={() => openEditDialog(role)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton edge="end" onClick={() => handleDelete(role.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  )
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{role.name}</Typography>
                      {role.system && (
                        <Tooltip title="System role - not editable">
                          <LockIcon fontSize="inherit" color="action" />
                        </Tooltip>
                      )}
                      {role.sensitive && <Chip size="small" color="warning" label="Sensitive" />}
                      {!role.assignableToHumans && <Chip size="small" color="default" label="Non-human" />}
                    </Stack>
                  }
                  slotProps={{ secondary: { component: 'div' } }}
                  secondary={
                    <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                        <Chip size="small" label={role.code} variant="outlined" />
                        {role.permissions.map((p) => (
                          <Chip key={p} size="small" label={p} />
                        ))}
                      </Stack>
                      {role.description && (
                        <Typography variant="body2" color="text.secondary">
                          {role.description}
                        </Typography>
                      )}
                    </Stack>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? `Edit ${editing.name}` : 'New Role'}</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Code"
              fullWidth
              required
              disabled={!!editing}
              helperText={editing ? 'Code cannot be changed after creation' : 'e.g. REGIONAL_COORDINATOR'}
              value={form.code}
              onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))}
            />
            <TextField
              label="Name"
              fullWidth
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
            <TextField
              label="Description"
              fullWidth
              multiline
              minRows={2}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
            <TextField
              label="Permissions"
              fullWidth
              multiline
              minRows={2}
              helperText="Comma-separated permission codes, e.g. assets:read, reports:read"
              value={form.permissions}
              onChange={(e) => setForm((f) => ({ ...f, permissions: e.target.value }))}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!form.name || (!editing && !form.code) || createRole.isPending || updateRole.isPending}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
