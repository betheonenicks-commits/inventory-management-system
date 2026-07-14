import { useState } from 'react'
import Alert from '@mui/material/Alert'
import AlertTitle from '@mui/material/AlertTitle'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import OutlinedInput from '@mui/material/OutlinedInput'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import type { SelectChangeEvent } from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import PersonOffIcon from '@mui/icons-material/PersonOff'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import type { ApiProblem } from '../../api/errors'
import { useRolesQuery } from '../roles/hooks/useRolesQuery'
import { useCreateUserMutation, useDeactivateUserMutation, useUsersQuery } from './hooks/useUsersQuery'
import type { User } from './types'

interface UserFormState {
  username: string
  password: string
  displayName: string
  email: string
  roleCodes: string[]
}

const EMPTY_FORM: UserFormState = { username: '', password: '', displayName: '', email: '', roleCodes: [] }

// US-USR-01/07/08. Person and org-scope pickers are deliberately not offered
// on this form: neither Person (FR-ORG-04) nor OrgNode (FR-ORG-01) has a list
// UI to pick from yet (EPIC-ORG isn't built beyond the minimal backend CRUD
// needed for asset assignment) - the backend fields exist and accept a UUID,
// but inventing a fake dropdown here would be worse than omitting the field.
export function UserListPage() {
  const usersQuery = useUsersQuery()
  const rolesQuery = useRolesQuery()
  const createUser = useCreateUserMutation()
  const deactivateUser = useDeactivateUserMutation()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'users:write')

  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<UserFormState>(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)
  const [deactivateTarget, setDeactivateTarget] = useState<User | null>(null)
  const [blockedProblem, setBlockedProblem] = useState<ApiProblem | null>(null)

  function openCreateDialog() {
    setForm(EMPTY_FORM)
    setError(null)
    setDialogOpen(true)
  }

  function handleRoleCodesChange(event: SelectChangeEvent<string[]>) {
    const value = event.target.value
    setForm((f) => ({ ...f, roleCodes: typeof value === 'string' ? value.split(',') : value }))
  }

  async function handleCreate() {
    setError(null)
    try {
      await createUser.mutateAsync({
        username: form.username,
        password: form.password,
        displayName: form.displayName,
        email: form.email || undefined,
        roleCodes: form.roleCodes,
      })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create user')
    }
  }

  async function handleConfirmDeactivate() {
    if (!deactivateTarget) return
    setBlockedProblem(null)
    try {
      await deactivateUser.mutateAsync({ id: deactivateTarget.id, version: deactivateTarget.version })
      setDeactivateTarget(null)
    } catch (err) {
      if (isApiProblem(err) && err.status === 409) {
        setBlockedProblem(err)
      } else {
        setDeactivateTarget(null)
      }
    }
  }

  return (
    <Box>
      <PageHeader
        title="Users"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
              New User
            </Button>
          )
        }
      />

      {usersQuery.isLoading && <LoadingSkeleton rows={6} />}
      {usersQuery.isError && <ErrorPanel error={usersQuery.error} onRetry={() => usersQuery.refetch()} />}

      {usersQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {usersQuery.data.map((user) => (
              <ListItem
                key={user.id}
                divider
                secondaryAction={
                  canWrite && user.status === 'ACTIVE' && (
                    <Button
                      size="small"
                      color="error"
                      startIcon={<PersonOffIcon />}
                      onClick={() => {
                        setDeactivateTarget(user)
                        setBlockedProblem(null)
                      }}
                    >
                      Deactivate
                    </Button>
                  )
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{user.displayName}</Typography>
                      <Chip
                        size="small"
                        label={user.status === 'ACTIVE' ? 'Active' : 'Deactivated'}
                        color={user.status === 'ACTIVE' ? 'success' : 'default'}
                      />
                    </Stack>
                  }
                  slotProps={{ secondary: { component: 'div' } }}
                  secondary={
                    <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                      <Typography variant="body2" color="text.secondary">
                        {user.username}
                        {user.email ? ` · ${user.email}` : ''}
                        {user.orgScopeNodeName ? ` · Scoped to ${user.orgScopeNodeName}` : ''}
                      </Typography>
                      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                        {user.roleCodes.map((code) => (
                          <Chip key={code} size="small" variant="outlined" label={code} />
                        ))}
                      </Stack>
                    </Stack>
                  }
                />
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New User</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Username"
              fullWidth
              required
              value={form.username}
              onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
            />
            <TextField
              label="Password"
              type="password"
              fullWidth
              required
              helperText="At least 8 characters"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
            />
            <TextField
              label="Display Name"
              fullWidth
              required
              value={form.displayName}
              onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
            />
            <TextField
              label="Email"
              fullWidth
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
            />
            <FormControl fullWidth required>
              <InputLabel id="role-codes-label">Roles</InputLabel>
              <Select
                labelId="role-codes-label"
                multiple
                input={<OutlinedInput label="Roles" />}
                value={form.roleCodes}
                onChange={handleRoleCodesChange}
                renderValue={(selected) => (
                  <Stack direction="row" spacing={0.5} sx={{ flexWrap: 'wrap' }}>
                    {selected.map((code) => (
                      <Chip key={code} size="small" label={code} />
                    ))}
                  </Stack>
                )}
              >
                {(rolesQuery.data ?? []).map((role) => (
                  <MenuItem key={role.code} value={role.code}>
                    {role.name}
                    {role.sensitive && ' (Super Admin only)'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={
              !form.username ||
              form.password.length < 8 ||
              !form.displayName ||
              form.roleCodes.length === 0 ||
              createUser.isPending
            }
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!deactivateTarget} onClose={() => setDeactivateTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Deactivate {deactivateTarget?.displayName}?</DialogTitle>
        <DialogContent>
          {blockedProblem ? (
            <Alert severity="warning">
              <AlertTitle>{blockedProblem.title}</AlertTitle>
              {blockedProblem.detail}
              {blockedProblem.blockingAssets && blockedProblem.blockingAssets.length > 0 && (
                <List dense sx={{ mt: 1 }}>
                  {blockedProblem.blockingAssets.map((asset) => (
                    <ListItem key={asset.assetId} disableGutters>
                      <ListItemText primary={`${asset.assetNumber} — ${asset.name}`} />
                    </ListItem>
                  ))}
                </List>
              )}
              {blockedProblem.resolutionActions && (
                <Stack spacing={0.25} sx={{ mt: 1 }}>
                  {blockedProblem.resolutionActions.map((action) => (
                    <Typography key={action} variant="caption" color="text.secondary">
                      {action}
                    </Typography>
                  ))}
                </Stack>
              )}
            </Alert>
          ) : (
            <DialogContentText>
              This will revoke sign-in access for {deactivateTarget?.username}. This cannot be undone from this
              screen.
            </DialogContentText>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeactivateTarget(null)}>{blockedProblem ? 'Close' : 'Cancel'}</Button>
          {!blockedProblem && (
            <Button color="error" variant="contained" onClick={handleConfirmDeactivate} disabled={deactivateUser.isPending}>
              Deactivate
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  )
}
