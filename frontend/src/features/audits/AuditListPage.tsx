import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { usePickableUsersQuery } from '../users/hooks/useUsersQuery'
import { useAssetCategoriesQuery } from '../assets/hooks/useAssetCategoriesQuery'
import { useOrgNodesQuery } from './hooks/useOrgNodesQuery'
import { useAuditsQuery, useCreateAuditMutation } from './hooks/useAuditsQuery'
import type { AuditStatus, AuditType } from './types'

const STATUS_TABS: Array<{ label: string; value: AuditStatus | undefined }> = [
  { label: 'All', value: undefined },
  { label: 'In Progress', value: 'IN_PROGRESS' },
  { label: 'Pending Approval', value: 'PENDING_APPROVAL' },
  { label: 'Closed', value: 'CLOSED' },
]

const STATUS_COLOR: Record<AuditStatus, 'info' | 'warning' | 'success'> = {
  IN_PROGRESS: 'info',
  PENDING_APPROVAL: 'warning',
  CLOSED: 'success',
}

type ScopeMode = 'ORG_NODE' | 'CATEGORY'

interface CreateFormState {
  name: string
  auditType: AuditType
  scopeMode: ScopeMode
  scopeOrgNodeId: string
  scopeCategoryId: string
  nominalApproverId: string
  scheduledDate: string
}

const EMPTY_FORM: CreateFormState = {
  name: '',
  auditType: 'SPOT_CHECK',
  scopeMode: 'ORG_NODE',
  scopeOrgNodeId: '',
  scopeCategoryId: '',
  nominalApproverId: '',
  scheduledDate: '',
}

// US-AUD-01/02/03: define an audit's type and scope (org node or category -
// an explicit asset-list scope also exists on the backend but has no picker
// here yet, same "real gap, not silently hidden" approach as other features
// that omit a control rather than fake it).
export function AuditListPage() {
  const [statusTab, setStatusTab] = useState(0)
  const status = STATUS_TABS[statusTab].value
  const auditsQuery = useAuditsQuery(status)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'audits:write')
  // Only fetched for the New Audit dialog's approver picker, and deliberately the
  // low-privilege /users/pickable endpoint (id + displayName only, no permission
  // gate) rather than the full users:read-gated list - AUDITOR holds audits:write
  // but not users:read, so the story's own "As an Auditor, define an audit" flow
  // 403'd on this picker until switched. Same fix EPIC-LIF's approver pickers
  // already needed for Inventory Manager/Department Head.
  const usersQuery = usePickableUsersQuery(canWrite)
  const categoriesQuery = useAssetCategoriesQuery()
  const orgNodesQuery = useOrgNodesQuery()
  const createAudit = useCreateAuditMutation()
  const navigate = useNavigate()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [form, setForm] = useState<CreateFormState>(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)

  function openCreateDialog() {
    setForm(EMPTY_FORM)
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    setError(null)
    try {
      const audit = await createAudit.mutateAsync({
        name: form.name,
        auditType: form.auditType,
        scopeOrgNodeId: form.scopeMode === 'ORG_NODE' ? form.scopeOrgNodeId : undefined,
        scopeCategoryId: form.scopeMode === 'CATEGORY' ? form.scopeCategoryId : undefined,
        nominalApproverId: form.nominalApproverId,
        scheduledDate: form.scheduledDate || undefined,
      })
      setDialogOpen(false)
      navigate(`/audits/${audit.id}`)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create audit')
    }
  }

  const canSubmitForm =
    form.name.trim().length > 0 &&
    form.nominalApproverId.length > 0 &&
    (form.scopeMode === 'ORG_NODE' ? form.scopeOrgNodeId.length > 0 : form.scopeCategoryId.length > 0)

  return (
    <Box>
      <PageHeader
        title="Audits"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
              New Audit
            </Button>
          )
        }
      />

      <Tabs value={statusTab} onChange={(_, v) => setStatusTab(v)} sx={{ mb: 2 }}>
        {STATUS_TABS.map((tab) => (
          <Tab key={tab.label} label={tab.label} />
        ))}
      </Tabs>

      {auditsQuery.isLoading && <LoadingSkeleton rows={6} />}
      {auditsQuery.isError && <ErrorPanel error={auditsQuery.error} onRetry={() => auditsQuery.refetch()} />}

      {auditsQuery.isSuccess && (
        <Paper variant="outlined">
          <List>
            {auditsQuery.data.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Typography color="text.secondary">No audits in this view yet.</Typography>
              </Box>
            )}
            {auditsQuery.data.map((audit) => (
              <ListItemButton key={audit.id} divider onClick={() => navigate(`/audits/${audit.id}`)}>
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <Typography variant="body1">{audit.name}</Typography>
                      <Chip size="small" label={audit.status.replace('_', ' ')} color={STATUS_COLOR[audit.status]} />
                      <Chip size="small" variant="outlined" label={audit.auditType.replace('_', ' ')} />
                    </Stack>
                  }
                  slotProps={{ secondary: { component: 'div' } }}
                  secondary={
                    <Typography variant="body2" color="text.secondary">
                      {audit.scopeOrgNodeName ?? audit.scopeCategoryName ?? 'Explicit asset list'}
                    </Typography>
                  }
                />
              </ListItemButton>
            ))}
          </List>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Audit</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              fullWidth
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
            <FormControl fullWidth required>
              <InputLabel id="audit-type-label">Type</InputLabel>
              <Select
                labelId="audit-type-label"
                label="Type"
                value={form.auditType}
                onChange={(e) => setForm((f) => ({ ...f, auditType: e.target.value as AuditType }))}
              >
                <MenuItem value="ANNUAL">Annual</MenuItem>
                <MenuItem value="SPOT_CHECK">Spot Check</MenuItem>
                <MenuItem value="BULK">Bulk</MenuItem>
              </Select>
            </FormControl>

            <Tabs
              value={form.scopeMode === 'ORG_NODE' ? 0 : 1}
              onChange={(_, v) => setForm((f) => ({ ...f, scopeMode: v === 0 ? 'ORG_NODE' : 'CATEGORY' }))}
            >
              <Tab label="Scope by Location" />
              <Tab label="Scope by Category" />
            </Tabs>

            {form.scopeMode === 'ORG_NODE' ? (
              <FormControl fullWidth required>
                <InputLabel id="scope-org-node-label">Location</InputLabel>
                <Select
                  labelId="scope-org-node-label"
                  label="Location"
                  value={form.scopeOrgNodeId}
                  onChange={(e) => setForm((f) => ({ ...f, scopeOrgNodeId: e.target.value }))}
                >
                  {(orgNodesQuery.data ?? []).map((node) => (
                    <MenuItem key={node.id} value={node.id}>
                      {node.name} ({node.levelName})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            ) : (
              <FormControl fullWidth required>
                <InputLabel id="scope-category-label">Category</InputLabel>
                <Select
                  labelId="scope-category-label"
                  label="Category"
                  value={form.scopeCategoryId}
                  onChange={(e) => setForm((f) => ({ ...f, scopeCategoryId: e.target.value }))}
                >
                  {(categoriesQuery.data ?? []).map((category) => (
                    <MenuItem key={category.id} value={category.id}>
                      {category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            <FormControl fullWidth required>
              <InputLabel id="approver-label">Approver</InputLabel>
              <Select
                labelId="approver-label"
                label="Approver"
                value={form.nominalApproverId}
                onChange={(e) => setForm((f) => ({ ...f, nominalApproverId: e.target.value }))}
              >
                {(usersQuery.data ?? []).map((user) => (
                  <MenuItem key={user.id} value={user.id}>
                    {user.displayName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Scheduled date (optional)"
              type="date"
              value={form.scheduledDate}
              onChange={(e) => setForm((f) => ({ ...f, scheduledDate: e.target.value }))}
              slotProps={{ inputLabel: { shrink: true } }}
              helperText="Shown on the dashboard's audit calendar"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleCreate} disabled={!canSubmitForm || createAudit.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
