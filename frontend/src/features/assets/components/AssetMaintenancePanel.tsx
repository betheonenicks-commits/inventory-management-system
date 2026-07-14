import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../../api/errors'
import { useAuthStore, hasPermission } from '../../../auth/authStore'
import {
  useCloseRepairMutation,
  useCreateMaintenanceScheduleMutation,
  useDeactivateMaintenanceScheduleMutation,
  useMaintenanceEventsQuery,
  useMaintenanceSchedulesQuery,
  useOpenRepairMutation,
  useRecordCorrectiveMaintenanceMutation,
  useRecordPreventiveMaintenanceMutation,
  useRepairsQuery,
} from '../../lifecycle/hooks/useMaintenanceQuery'
import type { Asset } from '../types'
import type { RepairEvent } from '../../lifecycle/types'

type DialogKind = 'repair' | 'close-repair' | 'schedule' | 'corrective' | null

/** US-LIF-06/07/08: repair events, preventive-maintenance schedules, and corrective maintenance. */
export function AssetMaintenancePanel({ asset }: { asset: Asset }) {
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')
  const repairsQuery = useRepairsQuery(asset.id)
  const schedulesQuery = useMaintenanceSchedulesQuery(asset.id)
  const eventsQuery = useMaintenanceEventsQuery(asset.id)

  const openRepair = useOpenRepairMutation(asset.id)
  const closeRepair = useCloseRepairMutation(asset.id)
  const createSchedule = useCreateMaintenanceScheduleMutation(asset.id)
  const deactivateSchedule = useDeactivateMaintenanceScheduleMutation(asset.id)
  const recordPreventive = useRecordPreventiveMaintenanceMutation(asset.id)
  const recordCorrective = useRecordCorrectiveMaintenanceMutation(asset.id)

  const [dialog, setDialog] = useState<DialogKind>(null)
  const [target, setTarget] = useState<RepairEvent | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Open-repair form
  const [vendorName, setVendorName] = useState('')
  const [repairReason, setRepairReason] = useState('')
  // Close-repair form
  const [actualReturnDate, setActualReturnDate] = useState('')
  const [actualCost, setActualCost] = useState('')
  // Schedule form
  const [intervalMonths, setIntervalMonths] = useState('6')
  const [nextDueDate, setNextDueDate] = useState('')
  // Corrective form
  const [correctiveNotes, setCorrectiveNotes] = useState('')

  function close() {
    setDialog(null)
    setTarget(null)
    setError(null)
  }

  async function handleOpenRepair() {
    setError(null)
    try {
      await openRepair.mutateAsync({ vendorName: vendorName || undefined, reason: repairReason })
      close()
      setVendorName('')
      setRepairReason('')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to log repair')
    }
  }

  async function handleCloseRepair() {
    if (!target) return
    setError(null)
    try {
      await closeRepair.mutateAsync({ id: target.id, actualReturnDate, actualCost: actualCost ? Number(actualCost) : undefined })
      close()
      setActualReturnDate('')
      setActualCost('')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to close repair')
    }
  }

  async function handleCreateSchedule() {
    setError(null)
    try {
      await createSchedule.mutateAsync({ intervalMonths: Number(intervalMonths), nextDueDate })
      close()
      setNextDueDate('')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to create schedule')
    }
  }

  async function handleRecordCorrective() {
    setError(null)
    try {
      await recordCorrective.mutateAsync({ notes: correctiveNotes })
      close()
      setCorrectiveNotes('')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to log corrective maintenance')
    }
  }

  const openRepairEvent = (repairsQuery.data ?? []).find((r) => r.status === 'OPEN')

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Maintenance</Typography>

      {/* Repairs */}
      <Box>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle2">Repairs</Typography>
          {canWrite && !openRepairEvent && (
            <Button size="small" onClick={() => setDialog('repair')}>
              Log Repair
            </Button>
          )}
        </Stack>
        <List dense>
          {(repairsQuery.data ?? []).map((repair) => (
            <ListItem
              key={repair.id}
              divider
              secondaryAction={
                canWrite &&
                repair.status === 'OPEN' && (
                  <Button size="small" onClick={() => { setTarget(repair); setDialog('close-repair') }}>
                    Close
                  </Button>
                )
              }
            >
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Chip size="small" color={repair.status === 'OPEN' ? 'warning' : 'success'} label={repair.status} />
                    <Typography variant="body2">{repair.vendorName ?? 'No vendor specified'}</Typography>
                  </Stack>
                }
                secondary={repair.reason}
              />
            </ListItem>
          ))}
          {(repairsQuery.data ?? []).length === 0 && (
            <Typography variant="body2" color="text.secondary">
              No repairs logged.
            </Typography>
          )}
        </List>
      </Box>

      <Divider />

      {/* Preventive schedules */}
      <Box>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle2">Preventive Maintenance Schedule</Typography>
          {canWrite && (
            <Button size="small" onClick={() => setDialog('schedule')}>
              New Schedule
            </Button>
          )}
        </Stack>
        <List dense>
          {(schedulesQuery.data ?? []).map((schedule) => (
            <ListItem
              key={schedule.id}
              divider
              secondaryAction={
                canWrite &&
                schedule.active && (
                  <Stack direction="row" spacing={0.5}>
                    <Button
                      size="small"
                      onClick={() => recordPreventive.mutate({ scheduleId: schedule.id })}
                    >
                      Complete
                    </Button>
                    <Button size="small" color="inherit" onClick={() => deactivateSchedule.mutate(schedule.id)}>
                      Deactivate
                    </Button>
                  </Stack>
                )
              }
            >
              <ListItemText
                primary={`Every ${schedule.intervalMonths} month(s)`}
                secondary={`Next due: ${schedule.nextDueDate}${schedule.active ? '' : ' (inactive)'}`}
              />
            </ListItem>
          ))}
          {(schedulesQuery.data ?? []).length === 0 && (
            <Typography variant="body2" color="text.secondary">
              No preventive maintenance scheduled.
            </Typography>
          )}
        </List>
      </Box>

      <Divider />

      {/* Maintenance event history + corrective logging */}
      <Box>
        <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle2">Maintenance History</Typography>
          {canWrite && (
            <Button size="small" onClick={() => setDialog('corrective')}>
              Log Corrective
            </Button>
          )}
        </Stack>
        <List dense>
          {(eventsQuery.data ?? []).map((event) => (
            <ListItem key={event.id} divider>
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Chip size="small" color={event.maintenanceType === 'PREVENTIVE' ? 'success' : 'warning'} label={event.maintenanceType} />
                    <Typography variant="body2">{new Date(event.performedAt).toLocaleDateString()}</Typography>
                  </Stack>
                }
                secondary={event.notes}
              />
            </ListItem>
          ))}
          {(eventsQuery.data ?? []).length === 0 && (
            <Typography variant="body2" color="text.secondary">
              No maintenance events recorded.
            </Typography>
          )}
        </List>
      </Box>

      {/* Dialogs */}
      <Dialog open={dialog === 'repair'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Log Repair</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Vendor" fullWidth value={vendorName} onChange={(e) => setVendorName(e.target.value)} />
            <TextField
              label="Reason"
              required
              fullWidth
              multiline
              minRows={2}
              value={repairReason}
              onChange={(e) => setRepairReason(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={handleOpenRepair} disabled={!repairReason || openRepair.isPending}>
            Log Repair
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'close-repair'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Close Repair</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Actual Return Date"
              type="date"
              required
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              value={actualReturnDate}
              onChange={(e) => setActualReturnDate(e.target.value)}
            />
            <TextField
              label="Actual Cost"
              type="number"
              fullWidth
              value={actualCost}
              onChange={(e) => setActualCost(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={handleCloseRepair} disabled={!actualReturnDate || closeRepair.isPending}>
            Close Repair
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'schedule'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>New Preventive Maintenance Schedule</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Interval (months)"
              type="number"
              required
              fullWidth
              value={intervalMonths}
              onChange={(e) => setIntervalMonths(e.target.value)}
            />
            <TextField
              label="Next Due Date"
              type="date"
              required
              fullWidth
              slotProps={{ inputLabel: { shrink: true } }}
              value={nextDueDate}
              onChange={(e) => setNextDueDate(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={handleCreateSchedule} disabled={!nextDueDate || createSchedule.isPending}>
            Create Schedule
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'corrective'} onClose={close} maxWidth="sm" fullWidth>
        <DialogTitle>Log Corrective Maintenance</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          <TextField
            label="Root-Cause Note"
            required
            fullWidth
            multiline
            minRows={2}
            sx={{ mt: 1 }}
            value={correctiveNotes}
            onChange={(e) => setCorrectiveNotes(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" onClick={handleRecordCorrective} disabled={!correctiveNotes || recordCorrective.isPending}>
            Log
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
