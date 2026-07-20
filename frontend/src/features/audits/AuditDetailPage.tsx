import { useParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import Grid from '@mui/material/Grid'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { useAuditQuery } from './hooks/useAuditsQuery'
import { AuditProgressPanel } from './components/AuditProgressPanel'
import { AuditAssignmentsPanel } from './components/AuditAssignmentsPanel'
import { AuditScanPanel } from './components/AuditScanPanel'
import { AuditExceptionsPanel } from './components/AuditExceptionsPanel'
import { AuditWorkflowPanel } from './components/AuditWorkflowPanel'
import type { AuditStatus } from './types'

const STATUS_COLOR: Record<AuditStatus, 'info' | 'warning' | 'success'> = {
  IN_PROGRESS: 'info',
  PENDING_APPROVAL: 'warning',
  CLOSED: 'success',
}

export function AuditDetailPage() {
  const { auditId } = useParams<{ auditId: string }>()
  const auditQuery = useAuditQuery(auditId)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'audits:write')

  if (auditQuery.isLoading) return <LoadingSkeleton rows={6} />
  if (auditQuery.isError) return <ErrorPanel error={auditQuery.error} onRetry={() => auditQuery.refetch()} />
  const audit = auditQuery.data!

  return (
    <Box>
      <PageHeader
        title={audit.name}
        actions={
          <Stack direction="row" spacing={1}>
            <Chip label={audit.status.replace('_', ' ')} color={STATUS_COLOR[audit.status]} />
            <Chip variant="outlined" label={audit.auditType.replace('_', ' ')} />
            {audit.samplingConfidenceLevel != null && (
              <Chip
                variant="outlined"
                color="info"
                label={`Sample of ${audit.samplingPopulationSize ?? '?'} · ${audit.samplingConfidenceLevel}% conf`}
              />
            )}
          </Stack>
        }
      />

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <AuditProgressPanel auditId={audit.id} />
          </Paper>

          {audit.status === 'IN_PROGRESS' && canWrite && (
            <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
              <AuditScanPanel auditId={audit.id} />
            </Paper>
          )}

          <Paper variant="outlined" sx={{ p: 2 }}>
            <AuditExceptionsPanel auditId={audit.id} />
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
            <AuditWorkflowPanel audit={audit} />
          </Paper>

          <Paper variant="outlined" sx={{ p: 2 }}>
            <AuditAssignmentsPanel auditId={audit.id} canWrite={canWrite} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
