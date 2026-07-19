import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { useTheme } from '@mui/material/styles'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { useCrossCycleTrendsQuery } from '../hooks/useAuditsQuery'
import { TrendChart } from './TrendChart'

// US-AUD-18: cross-cycle audit analytics - missing-rate and completion-time trends
// from system data, so BO-2/BO-3 movement is shown, not tallied by hand.
export function AuditTrendsPanel({ enabled = true }: { enabled?: boolean }) {
  const theme = useTheme()
  const query = useCrossCycleTrendsQuery(enabled)

  if (!enabled) return null

  return (
    <Paper variant="outlined" sx={{ mt: 2, p: 2 }}>
      <Typography variant="subtitle1" sx={{ mb: 0.5 }}>
        Audit trends (cross-cycle)
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Missing-rate and completion-time across completed audits in your scope. The reduction credits only
        formally reconciled assets (US-AUD-21).
      </Typography>

      {query.isLoading ? (
        <LoadingSkeleton rows={3} />
      ) : query.isError ? (
        <ErrorPanel error={query.error} onRetry={() => query.refetch()} />
      ) : (query.data ?? []).length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          No completed audit cycles yet — close an audit to start the trend.
        </Typography>
      ) : (
        (() => {
          const cycles = query.data ?? []
          const categories = cycles.map((c) => c.name)
          return (
            <Stack spacing={2.5}>
              <TrendChart
                title="Missing rate"
                unit="%"
                categories={categories}
                series={[
                  {
                    label: 'At close',
                    color: theme.palette.warning.main,
                    values: cycles.map((c) => c.missingRatePct),
                  },
                  {
                    label: 'After reconciliation',
                    color: theme.palette.primary.main,
                    values: cycles.map((c) => c.netMissingRatePct),
                  },
                ]}
              />
              <TrendChart
                title="Completion time"
                unit="d"
                categories={categories}
                series={[
                  {
                    label: 'Days',
                    color: theme.palette.info.main,
                    values: cycles.map((c) => c.completionDays),
                  },
                ]}
              />
              <TableContainer sx={{ overflowX: 'auto' }}>
                <Table size="small" aria-label="Cross-cycle audit metrics">
                  <TableHead>
                    <TableRow>
                      <TableCell>Cycle</TableCell>
                      <TableCell align="right">Expected</TableCell>
                      <TableCell align="right">Missing</TableCell>
                      <TableCell align="right">Reconciled</TableCell>
                      <TableCell align="right">Net&nbsp;missing</TableCell>
                      <TableCell align="right">Missing&nbsp;%</TableCell>
                      <TableCell align="right">Net&nbsp;%</TableCell>
                      <TableCell align="right">Days</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {cycles.map((c) => (
                      <TableRow key={c.auditId} hover>
                        <TableCell>{c.name}</TableCell>
                        <TableCell align="right">{c.expectedCount}</TableCell>
                        <TableCell align="right">{c.missingCount}</TableCell>
                        <TableCell align="right" sx={{ color: c.reconciledCount ? 'success.main' : undefined }}>
                          {c.reconciledCount}
                        </TableCell>
                        <TableCell align="right">{c.netMissingCount}</TableCell>
                        <TableCell align="right">{c.missingRatePct}%</TableCell>
                        <TableCell align="right">{c.netMissingRatePct}%</TableCell>
                        <TableCell align="right">{c.completionDays ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Box aria-hidden />
            </Stack>
          )
        })()
      )}
    </Paper>
  )
}
