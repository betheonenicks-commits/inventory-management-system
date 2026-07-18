import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import IconButton from '@mui/material/IconButton'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import DownloadIcon from '@mui/icons-material/Download'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { isApiProblem } from '../../api/errors'
import { fetchAssetCategories } from '../../api/assets/assetCategoryApi'
import { fetchAssetStatuses } from '../../api/assets/assetStatusApi'
import { fetchOrgNodes } from '../../api/org/orgNodeApi'
import {
  createAdHocReport,
  deleteAdHocReport,
  downloadAdHocReport,
  fetchAdHocFields,
  fetchAdHocReports,
  runAdHocReport,
} from '../../api/reports/reportApi'
import type { AdHocReport, ExportFormat } from '../../api/reports/reportApi'
import type { TabularReport } from './types'

/**
 * US-RPT-15: build a custom report from the server's field catalog + the
 * standard asset filters, save it, rerun on demand. Results render through
 * the parent page's generic table (onRun); downloads honor the page's
 * format picker. Definitions are the caller's own only.
 */
export function AdHocReportsPanel({ onRun, exportFormat }: {
  onRun: (report: TabularReport | null, error?: string) => void
  exportFormat: ExportFormat
}) {
  const queryClient = useQueryClient()
  const reportsQuery = useQuery({ queryKey: ['RPT', 'adhoc'], queryFn: fetchAdHocReports })
  const removeReport = useMutation({
    mutationFn: deleteAdHocReport,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['RPT', 'adhoc'] }),
  })

  const [builderOpen, setBuilderOpen] = useState(false)
  const [name, setName] = useState('')
  const [selectedFields, setSelectedFields] = useState<string[]>(['assetNumber', 'name'])
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [statusId, setStatusId] = useState('')
  const [orgNodeId, setOrgNodeId] = useState('')
  const [purchasedFrom, setPurchasedFrom] = useState('')
  const [purchasedTo, setPurchasedTo] = useState('')
  const [builderError, setBuilderError] = useState<string | null>(null)

  // Fetch the palette + filter pickers only while the builder is open - the
  // enabled-gating discipline every prior permission bug taught.
  const fieldsQuery = useQuery({ queryKey: ['RPT', 'adhoc-fields'], queryFn: fetchAdHocFields, enabled: builderOpen })
  const categoriesQuery = useQuery({ queryKey: ['AST', 'categories'], queryFn: fetchAssetCategories, enabled: builderOpen })
  const statusesQuery = useQuery({ queryKey: ['AST', 'statuses'], queryFn: fetchAssetStatuses, enabled: builderOpen })
  const orgNodesQuery = useQuery({ queryKey: ['ORG', 'orgNodes'], queryFn: fetchOrgNodes, enabled: builderOpen })

  function toggleField(key: string) {
    setSelectedFields((current) =>
      current.includes(key) ? current.filter((k) => k !== key) : [...current, key],
    )
  }

  async function save() {
    setBuilderError(null)
    try {
      await createAdHocReport({
        name,
        fields: selectedFields,
        query: query || undefined,
        categoryId: categoryId || undefined,
        statusId: statusId || undefined,
        orgNodeId: orgNodeId || undefined,
        purchasedFrom: purchasedFrom || undefined,
        purchasedTo: purchasedTo || undefined,
      })
      setBuilderOpen(false)
      setName('')
      queryClient.invalidateQueries({ queryKey: ['RPT', 'adhoc'] })
    } catch (err) {
      setBuilderError(isApiProblem(err) ? err.detail : 'Failed to save the report')
    }
  }

  async function run(report: AdHocReport) {
    try {
      onRun(await runAdHocReport(report.id))
    } catch (err) {
      onRun(null, isApiProblem(err) ? err.detail : 'Failed to run the report')
    }
  }

  async function download(report: AdHocReport) {
    try {
      await downloadAdHocReport(report, exportFormat)
    } catch (err) {
      onRun(null, isApiProblem(err) ? err.detail : `Failed to download the ${exportFormat.toUpperCase()}`)
    }
  }

  return (
    <Paper variant="outlined" sx={{ mt: 2, p: 2 }}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Typography variant="subtitle1">My custom reports</Typography>
        <Button size="small" variant="outlined" startIcon={<AddIcon />} onClick={() => setBuilderOpen(true)}>
          Build custom report…
        </Button>
      </Stack>

      {(reportsQuery.data ?? []).length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          Pick fields and filters once, save, and rerun with current data whenever you need it - no development
          request required.
        </Typography>
      ) : (
        <List dense disablePadding>
          {(reportsQuery.data ?? []).map((r) => (
            <ListItem
              key={r.id}
              divider
              secondaryAction={
                <Stack direction="row" spacing={0.5}>
                  <IconButton size="small" aria-label={`Run ${r.name}`} onClick={() => run(r)}>
                    <PlayArrowIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" aria-label={`Download ${r.name}`} onClick={() => download(r)}>
                    <DownloadIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" aria-label={`Delete ${r.name}`} onClick={() => removeReport.mutate(r.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              }
            >
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                    <Typography variant="body2">{r.name}</Typography>
                    <Chip size="small" label={`${r.fields.length} column${r.fields.length === 1 ? '' : 's'}`} />
                  </Stack>
                }
                secondary={r.query ? `Text filter: "${r.query}"` : undefined}
              />
            </ListItem>
          ))}
        </List>
      )}

      <Dialog open={builderOpen} onClose={() => setBuilderOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Build a custom report</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {builderError && <Alert severity="error">{builderError}</Alert>}
            <TextField label="Report name" required value={name} onChange={(e) => setName(e.target.value)}
              sx={{ maxWidth: 420 }} />

            <Typography variant="subtitle2">Columns (in the order selected)</Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
              {(fieldsQuery.data ?? []).map((f) => (
                <FormControlLabel
                  key={f.key}
                  control={<Checkbox size="small" checked={selectedFields.includes(f.key)}
                    onChange={() => toggleField(f.key)} />}
                  label={f.label}
                />
              ))}
            </Box>

            <Typography variant="subtitle2">Filters (all optional)</Typography>
            <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
              <TextField label="Text search" value={query} onChange={(e) => setQuery(e.target.value)}
                sx={{ minWidth: 200 }} />
              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="adhoc-category-label">Category</InputLabel>
                <Select labelId="adhoc-category-label" label="Category" value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}>
                  <MenuItem value="">Any</MenuItem>
                  {(categoriesQuery.data ?? []).map((c) => (
                    <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl sx={{ minWidth: 180 }}>
                <InputLabel id="adhoc-status-label">Status</InputLabel>
                <Select labelId="adhoc-status-label" label="Status" value={statusId}
                  onChange={(e) => setStatusId(e.target.value)}>
                  <MenuItem value="">Any</MenuItem>
                  {(statusesQuery.data ?? []).map((s) => (
                    <MenuItem key={s.id} value={s.id}>{s.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl sx={{ minWidth: 220 }}>
                <InputLabel id="adhoc-location-label">Location</InputLabel>
                <Select labelId="adhoc-location-label" label="Location" value={orgNodeId}
                  onChange={(e) => setOrgNodeId(e.target.value)}>
                  <MenuItem value="">Any</MenuItem>
                  {(orgNodesQuery.data ?? []).map((n) => (
                    <MenuItem key={n.id} value={n.id}>{n.levelName}: {n.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <TextField label="Purchased from" type="date" value={purchasedFrom}
                onChange={(e) => setPurchasedFrom(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
              <TextField label="Purchased to" type="date" value={purchasedTo}
                onChange={(e) => setPurchasedTo(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBuilderOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={save} disabled={!name.trim() || selectedFields.length === 0}>
            Save report
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  )
}
