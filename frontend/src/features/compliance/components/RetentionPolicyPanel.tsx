import { useState } from 'react'
import Alert from '@mui/material/Alert'
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
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../../api/errors'
import { useRetentionPoliciesQuery, useSaveRetentionPolicyMutation, usePurgeSecurityEventLogMutation } from '../hooks/useRetentionPolicyQuery'
import type { RetentionEntityType, RetentionExpiryAction } from '../types'

const ENTITY_TYPES: RetentionEntityType[] = ['SECURITY_EVENT_LOG', 'DISPOSED_ASSET', 'PERSON', 'ASSET_HISTORY_EVENT', 'AUDIT_RECORD']
const EXPIRY_ACTIONS: RetentionExpiryAction[] = ['DELETE', 'ANONYMIZE', 'HOLD_ELIGIBLE']

// BRD §5.4 floors - shown as helper text only; the backend is the real enforcement.
const FLOOR_DAYS: Partial<Record<RetentionEntityType, number>> = {
  SECURITY_EVENT_LOG: 2555,
  DISPOSED_ASSET: 1095,
}

/** US-CMP-01: per-entity-type retention policy, plus a manual purge trigger for security_event_log. */
export function RetentionPolicyPanel({ canWrite }: { canWrite: boolean }) {
  const policiesQuery = useRetentionPoliciesQuery()
  const savePolicy = useSaveRetentionPolicyMutation()
  const purge = usePurgeSecurityEventLogMutation()

  const [entityType, setEntityType] = useState<RetentionEntityType>('SECURITY_EVENT_LOG')
  const [retentionPeriodDays, setRetentionPeriodDays] = useState('')
  const [expiryAction, setExpiryAction] = useState<RetentionExpiryAction>('DELETE')
  const [error, setError] = useState<string | null>(null)
  const [purgeResult, setPurgeResult] = useState<string | null>(null)

  async function handleSave() {
    setError(null)
    try {
      await savePolicy.mutateAsync({ entityType, retentionPeriodDays: Number(retentionPeriodDays), expiryAction })
      setRetentionPeriodDays('')
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save retention policy')
    }
  }

  async function handlePurge() {
    setError(null)
    setPurgeResult(null)
    try {
      const result = await purge.mutateAsync()
      setPurgeResult(`Purged ${result.deletedCount} security_event_log row(s) older than the configured retention period.`)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to run purge')
    }
  }

  if (policiesQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (policiesQuery.isError) return <ErrorPanel error={policiesQuery.error} onRetry={() => policiesQuery.refetch()} />

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Retention Policies</Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {purgeResult && (
        <Alert severity="success" onClose={() => setPurgeResult(null)}>
          {purgeResult}
        </Alert>
      )}

      <List dense>
        {(policiesQuery.data ?? []).map((policy) => (
          <ListItem key={policy.id} divider>
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <Typography variant="body2">{policy.entityType.replace(/_/g, ' ')}</Typography>
                  <Chip size="small" label={policy.expiryAction} />
                </Stack>
              }
              secondary={`${policy.retentionPeriodDays} days`}
            />
          </ListItem>
        ))}
        {(policiesQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No retention policies configured yet.
          </Typography>
        )}
      </List>

      {canWrite && (
        <>
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel id="entity-type-label">Entity Type</InputLabel>
              <Select
                labelId="entity-type-label"
                label="Entity Type"
                value={entityType}
                onChange={(e) => setEntityType(e.target.value as RetentionEntityType)}
              >
                {ENTITY_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>
                    {t.replace(/_/g, ' ')}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Retention Period (days)"
              size="small"
              type="number"
              value={retentionPeriodDays}
              onChange={(e) => setRetentionPeriodDays(e.target.value)}
              helperText={FLOOR_DAYS[entityType] ? `BRD floor: ${FLOOR_DAYS[entityType]} days` : undefined}
            />
            <FormControl size="small" sx={{ minWidth: 160 }}>
              <InputLabel id="expiry-action-label">Expiry Action</InputLabel>
              <Select
                labelId="expiry-action-label"
                label="Expiry Action"
                value={expiryAction}
                onChange={(e) => setExpiryAction(e.target.value as RetentionExpiryAction)}
              >
                {EXPIRY_ACTIONS.map((a) => (
                  <MenuItem key={a} value={a}>
                    {a}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="contained" onClick={handleSave} disabled={!retentionPeriodDays || savePolicy.isPending}>
              Save
            </Button>
          </Stack>

          <Box>
            <Button variant="outlined" color="warning" onClick={handlePurge} disabled={purge.isPending}>
              Run security_event_log Purge Now
            </Button>
          </Box>
        </>
      )}
    </Stack>
  )
}
