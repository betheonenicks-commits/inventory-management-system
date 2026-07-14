import Chip from '@mui/material/Chip'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { useAuditExceptionsQuery } from '../hooks/useAuditsQuery'
import type { FindingStatus } from '../types'

const STATUS_COLOR: Record<FindingStatus, 'success' | 'error' | 'warning'> = {
  VERIFIED: 'success',
  MISSING: 'error',
  OUT_OF_SCOPE: 'warning',
  SCOPE_CHANGED: 'warning',
}

/** US-AUD-16: everything that wasn't clean - Missing, Damaged, Out of Scope, Scope Changed. */
export function AuditExceptionsPanel({ auditId }: { auditId: string }) {
  const exceptionsQuery = useAuditExceptionsQuery(auditId)

  if (exceptionsQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (exceptionsQuery.isError) return <ErrorPanel error={exceptionsQuery.error} onRetry={() => exceptionsQuery.refetch()} />
  const report = exceptionsQuery.data!

  return (
    <Stack spacing={1}>
      <Typography variant="subtitle1">Exceptions</Typography>
      {!report.hasExceptions ? (
        <Typography variant="body2" color="text.secondary">
          No exceptions - every scanned asset in scope came back clean.
        </Typography>
      ) : (
        <List dense>
          {report.findings.map((finding) => (
            <ListItem key={finding.id} divider>
              <ListItemText
                primary={
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Typography variant="body2">
                      {finding.assetNumber} — {finding.assetName}
                    </Typography>
                    <Chip size="small" label={finding.status.replace('_', ' ')} color={STATUS_COLOR[finding.status]} />
                    {finding.condition && <Chip size="small" variant="outlined" label={finding.condition.replace('_', ' ')} />}
                  </Stack>
                }
                slotProps={{ secondary: { component: 'div' } }}
                secondary={finding.remarks ?? undefined}
              />
            </ListItem>
          ))}
        </List>
      )}
    </Stack>
  )
}
