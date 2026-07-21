import { useState } from 'react'
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { EmptyState } from '../../components/common/EmptyState'
import { usePickableUsersQuery } from '../users/hooks/useUsersQuery'
import { useSecurityEventsQuery } from './hooks/useSecurityEventsQuery'
import { SECURITY_EVENT_TYPES } from '../../api/security/securityEventApi'
import type { SecurityEventType } from '../../api/security/securityEventApi'

const SEVERE_EVENTS = new Set<SecurityEventType>([
  'LOGIN_FAILURE', 'PERMISSION_DENIED', 'ACCOUNT_LOCKED', 'REFRESH_TOKEN_REUSE_DETECTED', 'SESSION_EXPIRED',
])

/**
 * US-SEC-11: search/filter the Security & Access Log - the search/filter
 * endpoint (SecurityEventController) existed with no frontend page reaching
 * it at all. A formal exportable report over the same data already exists
 * separately (US-RPT-14, the Reports page's "Security & Access Log" report)
 * - this is the live, interactive, paginated view; that is the
 * generate-and-download one.
 */
export function SecurityEventLogPage() {
  const [userId, setUserId] = useState('')
  const [eventType, setEventType] = useState<SecurityEventType | ''>('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(50)

  const usersQuery = usePickableUsersQuery()
  const eventsQuery = useSecurityEventsQuery({
    userId: userId || undefined,
    eventType: eventType || undefined,
    from: from ? new Date(from).toISOString() : undefined,
    to: to ? new Date(to).toISOString() : undefined,
    page,
    size,
  })

  function userLabel(id: string | null) {
    if (!id) return '—'
    return (usersQuery.data ?? []).find((u) => u.id === id)?.displayName ?? id
  }

  return (
    <Box>
      <PageHeader title="Security & Access Log" />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        For a formal, downloadable copy of this log, use the "Security &amp; Access Log" report on the Reports page.
      </Typography>

      <Stack direction="row" spacing={2} useFlexGap sx={{ mb: 2, flexWrap: 'wrap' }}>
        <TextField
          select
          label="User"
          size="small"
          value={userId}
          onChange={(e) => {
            setUserId(e.target.value)
            setPage(0)
          }}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">All users</MenuItem>
          {(usersQuery.data ?? []).map((u) => (
            <MenuItem key={u.id} value={u.id}>
              {u.displayName}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          select
          label="Event type"
          size="small"
          value={eventType}
          onChange={(e) => {
            setEventType(e.target.value as SecurityEventType | '')
            setPage(0)
          }}
          sx={{ minWidth: 220 }}
        >
          <MenuItem value="">All event types</MenuItem>
          {SECURITY_EVENT_TYPES.map((t) => (
            <MenuItem key={t} value={t}>
              {t.replace(/_/g, ' ')}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label="From"
          type="datetime-local"
          size="small"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value)
            setPage(0)
          }}
          slotProps={{ inputLabel: { shrink: true } }}
          sx={{ minWidth: 200 }}
        />
        <TextField
          label="To"
          type="datetime-local"
          size="small"
          value={to}
          onChange={(e) => {
            setTo(e.target.value)
            setPage(0)
          }}
          slotProps={{ inputLabel: { shrink: true } }}
          sx={{ minWidth: 200 }}
        />
      </Stack>

      {eventsQuery.isLoading && <LoadingSkeleton rows={8} />}
      {eventsQuery.isError && <ErrorPanel error={eventsQuery.error} onRetry={() => eventsQuery.refetch()} />}

      {eventsQuery.isSuccess && eventsQuery.data.data.length === 0 && (
        <EmptyState title="No events match these filters" />
      )}

      {eventsQuery.isSuccess && eventsQuery.data.data.length > 0 && (
        <Paper variant="outlined">
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>When</TableCell>
                  <TableCell>Event</TableCell>
                  <TableCell>User</TableCell>
                  <TableCell>Username attempted</TableCell>
                  <TableCell>Detail</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {eventsQuery.data.data.map((event) => (
                  <TableRow key={event.id}>
                    <TableCell>{new Date(event.createdAt).toLocaleString()}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        color={SEVERE_EVENTS.has(event.eventType) ? 'warning' : 'default'}
                        variant={SEVERE_EVENTS.has(event.eventType) ? 'filled' : 'outlined'}
                        label={event.eventType.replace(/_/g, ' ')}
                      />
                    </TableCell>
                    <TableCell>{userLabel(event.actorUserId)}</TableCell>
                    <TableCell>{event.usernameAttempted ?? '—'}</TableCell>
                    <TableCell>{event.detail ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={eventsQuery.data.page.totalElements}
            page={eventsQuery.data.page.number}
            rowsPerPage={eventsQuery.data.page.size}
            rowsPerPageOptions={[25, 50, 100]}
            onPageChange={(_, newPage) => setPage(newPage)}
            onRowsPerPageChange={(e) => {
              setSize(Number(e.target.value))
              setPage(0)
            }}
          />
        </Paper>
      )}
    </Box>
  )
}
