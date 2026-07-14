import Box from '@mui/material/Box'
import Grid from '@mui/material/Grid'
import LinearProgress from '@mui/material/LinearProgress'
import Stack from '@mui/material/Stack'
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
    </Stack>
  )
}
