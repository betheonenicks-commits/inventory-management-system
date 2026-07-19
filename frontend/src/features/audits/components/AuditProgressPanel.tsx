import Box from '@mui/material/Box'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid'
import LinearProgress from '@mui/material/LinearProgress'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { useAuditProgressQuery } from '../hooks/useAuditsQuery'

function Stat({ label, value, color }: { label: string; value: number; color?: string }) {
  return (
    <Box>
      <Typography variant="h5" sx={{ color }}>
        {value}
      </Typography>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
    </Box>
  )
}

// US-AUD-08: real-time expected-vs-verified progress.
export function AuditProgressPanel({ auditId }: { auditId: string }) {
  const progressQuery = useAuditProgressQuery(auditId)

  if (progressQuery.isLoading) return <LoadingSkeleton rows={2} />
  if (progressQuery.isError) return <ErrorPanel error={progressQuery.error} onRetry={() => progressQuery.refetch()} />
  const progress = progressQuery.data!

  return (
    <Stack spacing={2}>
      <Box>
        <Stack direction="row" sx={{ justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="body2">Progress</Typography>
          <Typography variant="body2">{progress.percentComplete.toFixed(0)}%</Typography>
        </Stack>
        <LinearProgress variant="determinate" value={Math.min(progress.percentComplete, 100)} />
      </Box>
      <Grid container spacing={2}>
        <Grid size={4}>
          <Stat label="Expected" value={progress.expectedCount} />
        </Grid>
        <Grid size={4}>
          <Stat label="Verified" value={progress.verifiedCount} color="success.main" />
        </Grid>
        <Grid size={4}>
          <Stat label="Missing" value={progress.missingCount} color="error.main" />
        </Grid>
        <Grid size={4}>
          <Stat label="Out of Scope" value={progress.outOfScopeCount} color="warning.main" />
        </Grid>
        <Grid size={4}>
          <Stat label="Scope Changed" value={progress.scopeChangedCount} color="warning.main" />
        </Grid>
      </Grid>
      {/* US-AUD-03: a bulk audit spanning several locations breaks its progress down
          by sub-scope, not just one flat total. Shown only when there's more than one
          location - a single-location audit is already fully described above. */}
      {progress.subScopes.length > 1 && (
        <Box>
          <Divider sx={{ mb: 1 }} />
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
            By sub-scope (location)
          </Typography>
          <Table size="small" aria-label="Progress by sub-scope">
            <TableHead>
              <TableRow>
                <TableCell>Location</TableCell>
                <TableCell align="right">%</TableCell>
                <TableCell align="right">Expected</TableCell>
                <TableCell align="right">Verified</TableCell>
                <TableCell align="right">Missing</TableCell>
                <TableCell align="right">Out&nbsp;of&nbsp;Scope</TableCell>
                <TableCell align="right">Scope&nbsp;Changed</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {progress.subScopes.map((sub) => (
                <TableRow key={sub.orgNodeId}>
                  <TableCell>
                    {sub.orgNodeName}
                    {sub.orgNodeCode ? (
                      <Typography component="span" variant="caption" color="text.secondary">
                        {' '}
                        ({sub.orgNodeCode})
                      </Typography>
                    ) : null}
                  </TableCell>
                  <TableCell align="right">{sub.percentComplete.toFixed(0)}%</TableCell>
                  <TableCell align="right">{sub.expectedCount}</TableCell>
                  <TableCell align="right" sx={{ color: 'success.main' }}>
                    {sub.verifiedCount}
                  </TableCell>
                  <TableCell align="right" sx={{ color: sub.missingCount ? 'error.main' : undefined }}>
                    {sub.missingCount}
                  </TableCell>
                  <TableCell align="right" sx={{ color: sub.outOfScopeCount ? 'warning.main' : undefined }}>
                    {sub.outOfScopeCount}
                  </TableCell>
                  <TableCell align="right" sx={{ color: sub.scopeChangedCount ? 'warning.main' : undefined }}>
                    {sub.scopeChangedCount}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
    </Stack>
  )
}
