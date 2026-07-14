import { useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { useUsersQuery } from '../../users/hooks/useUsersQuery'
import {
  useAssignAuditorMutation,
  useAuditAssignmentsQuery,
  useUnassignAuditorMutation,
} from '../hooks/useAuditAssignmentMutations'

/** US-AUD-02: assign one or more auditors to an audit, optionally splitting scope. */
export function AuditAssignmentsPanel({ auditId, canWrite }: { auditId: string; canWrite: boolean }) {
  const assignmentsQuery = useAuditAssignmentsQuery(auditId)
  // Gated behind canWrite - a read-only viewer without users:read (e.g. a
  // Department Head, who can approve but not assign) must not trigger this
  // call at all. Found via browser-testing: it 403'd for exactly that role.
  const usersQuery = useUsersQuery(canWrite)
  const assignAuditor = useAssignAuditorMutation(auditId)
  const unassignAuditor = useUnassignAuditorMutation(auditId)
  const [selectedAuditor, setSelectedAuditor] = useState('')

  const auditors = (usersQuery.data ?? []).filter((u) => u.roleCodes.includes('AUDITOR'))

  async function handleAssign() {
    if (!selectedAuditor) return
    await assignAuditor.mutateAsync({ auditorUserId: selectedAuditor })
    setSelectedAuditor('')
  }

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Auditors</Typography>

      {assignmentsQuery.isError && (
        <ErrorPanel error={assignmentsQuery.error} onRetry={() => assignmentsQuery.refetch()} />
      )}

      <List dense>
        {(assignmentsQuery.data ?? []).map((assignment) => (
          <ListItem
            key={assignment.id}
            secondaryAction={
              canWrite &&
              assignment.active && (
                <Button size="small" color="error" onClick={() => unassignAuditor.mutate(assignment.id)}>
                  Unassign
                </Button>
              )
            }
          >
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Typography variant="body2">{assignment.auditorUsername}</Typography>
                  {!assignment.active && <Chip size="small" label="Ended" />}
                </Stack>
              }
              secondary={assignment.subScope ?? undefined}
            />
          </ListItem>
        ))}
        {(assignmentsQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No auditors assigned yet.
          </Typography>
        )}
      </List>

      {canWrite && (
        <Box>
          <Stack direction="row" spacing={1}>
            <FormControl size="small" sx={{ minWidth: 220 }}>
              <InputLabel id="assign-auditor-label">Assign auditor</InputLabel>
              <Select
                labelId="assign-auditor-label"
                label="Assign auditor"
                value={selectedAuditor}
                onChange={(e) => setSelectedAuditor(e.target.value)}
              >
                {auditors.map((user) => (
                  <MenuItem key={user.id} value={user.id}>
                    {user.displayName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="outlined" onClick={handleAssign} disabled={!selectedAuditor || assignAuditor.isPending}>
              Assign
            </Button>
          </Stack>
        </Box>
      )}
    </Stack>
  )
}
