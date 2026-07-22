import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useSystemHealthQuery } from './hooks/useSystemHealthQuery'

function statusColor(status: string): 'success' | 'error' | 'warning' | 'default' {
  if (status === 'UP') return 'success'
  if (status === 'DOWN' || status === 'OUT_OF_SERVICE') return 'error'
  if (status === 'UNKNOWN') return 'warning'
  return 'default'
}

/**
 * US-USR-05 (AC-USR-05-H): the System Operator's technical-configuration view.
 * Deliberately business-data-free - it shows subsystem health (database, disk,
 * etc.), nothing about assets, valuations, or people.
 */
export function SystemHealthPage() {
  const healthQuery = useSystemHealthQuery()

  return (
    <Box>
      <PageHeader title="System Health" />

      {healthQuery.isLoading && <LoadingSkeleton rows={4} />}
      {healthQuery.isError && <ErrorPanel error={healthQuery.error} onRetry={() => healthQuery.refetch()} />}

      {healthQuery.isSuccess && (
        <Paper variant="outlined" sx={{ p: 3, maxWidth: 560 }}>
          <Stack spacing={2}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <Typography variant="subtitle1">Overall status</Typography>
              <Chip label={healthQuery.data.status} color={statusColor(healthQuery.data.status)} size="small" />
            </Stack>
            <Typography variant="body2" color="text.secondary">
              Last checked {new Date(healthQuery.data.checkedAt).toLocaleString()} · refreshes automatically.
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Component</TableCell>
                  <TableCell align="right">Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {Object.entries(healthQuery.data.components).map(([name, status]) => (
                  <TableRow key={name}>
                    <TableCell>{name}</TableCell>
                    <TableCell align="right">
                      <Chip label={status} color={statusColor(status)} size="small" />
                    </TableCell>
                  </TableRow>
                ))}
                {Object.keys(healthQuery.data.components).length === 0 && (
                  <TableRow>
                    <TableCell colSpan={2}>
                      <Typography variant="body2" color="text.secondary">
                        No component detail available.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Stack>
        </Paper>
      )}
    </Box>
  )
}
