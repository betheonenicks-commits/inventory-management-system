import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
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
import FormControlLabel from '@mui/material/FormControlLabel'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
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
import { fetchSampleSizePreview } from '../../api/audits/auditApi'
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
  // US-AUD-20: sampling is opt-in; off = full 100% verification (never assumed).
  samplingEnabled: boolean
  samplingConfidenceLevel: number
}

const EMPTY_FORM: CreateFormState = {
  name: '',
  auditType: 'SPOT_CHECK',
  scopeMode: 'ORG_NODE',
  scopeOrgNodeId: '',
  scopeCategoryId: '',
  nominalApproverId: '',
  scheduledDate: '',
  samplingEnabled: false,
  samplingConfidenceLevel: 95,
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

  // US-AUD-20: live sample-size preview once sampling is on and a scope is chosen.
  const samplingScopeChosen =
    form.scopeMode === 'ORG_NODE' ? form.scopeOrgNodeId.length > 0 : form.scopeCategoryId.length > 0
  const samplePreviewQuery = useQuery({
    queryKey: [
      'AUD',
      'sample-preview',
      form.scopeMode,
      form.scopeOrgNodeId,
      form.scopeCategoryId,
      form.samplingConfidenceLevel,
    ],
    queryFn: () =>
      fetchSampleSizePreview({
        scopeOrgNodeId: form.scopeMode === 'ORG_NODE' ? form.scopeOrgNodeId : undefined,
        scopeCategoryId: form.scopeMode === 'CATEGORY' ? form.scopeCategoryId : undefined,
        confidenceLevel: form.samplingConfidenceLevel,
      }),
    enabled: dialogOpen && form.samplingEnabled && samplingScopeChosen,
  })

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
        // US-AUD-20: only send sampling params when the auditor opts in.
        samplingConfidenceLevel: form.samplingEnabled ? form.samplingConfidenceLevel : undefined,
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

            {/* US-AUD-20: optional statistical sampling; off by default = full 100% verification. */}
            <Box>
              <FormControlLabel
                control={
                  <Switch
                    checked={form.samplingEnabled}
                    onChange={(e) => setForm((f) => ({ ...f, samplingEnabled: e.target.checked }))}
                  />
                }
                label="Statistical sampling (verify a representative subset, not 100%)"
              />
              {form.samplingEnabled && (
                <Stack spacing={1} sx={{ mt: 1 }}>
                  <FormControl fullWidth size="small">
                    <InputLabel id="confidence-label">Confidence level</InputLabel>
                    <Select
                      labelId="confidence-label"
                      label="Confidence level"
                      value={form.samplingConfidenceLevel}
                      onChange={(e) =>
                        setForm((f) => ({ ...f, samplingConfidenceLevel: Number(e.target.value) }))
                      }
                    >
                      <MenuItem value={90}>90%</MenuItem>
                      <MenuItem value={95}>95%</MenuItem>
                      <MenuItem value={99}>99%</MenuItem>
                    </Select>
                  </FormControl>
                  {!samplingScopeChosen ? (
                    <Alert severity="info" variant="outlined">
                      Choose a scope above to preview the sample size.
                    </Alert>
                  ) : samplePreviewQuery.isLoading ? (
                    <Typography variant="body2" color="text.secondary">
                      Calculating sample size…
                    </Typography>
                  ) : samplePreviewQuery.isError ? (
                    <Alert severity="warning" variant="outlined">
                      Could not compute the sample size for this scope.
                    </Alert>
                  ) : samplePreviewQuery.data ? (
                    <Alert severity="success" variant="outlined">
                      Will verify a sample of <strong>{samplePreviewQuery.data.sampleSize}</strong> of{' '}
                      {samplePreviewQuery.data.populationSize} assets ({form.samplingConfidenceLevel}% confidence,{' '}
                      {samplePreviewQuery.data.marginOfError}% margin).
                    </Alert>
                  ) : null}
                </Stack>
              )}
            </Box>
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
