import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Switch from '@mui/material/Switch'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { isApiProblem } from '../../api/errors'
import {
  fetchNotificationPreferences,
  updateNotificationPreference,
} from '../../api/notifications/notificationApi'
import type { NotificationEventType } from '../../api/notifications/notificationApi'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { PageHeader } from '../../components/common/PageHeader'

const LABELS: Record<NotificationEventType, string> = {
  UPCOMING_AUDIT: 'Upcoming audit',
  OVERDUE_AUDIT: 'Overdue audit',
  EXPIRY: 'Warranty / insurance expiry',
  MAINTENANCE_DUE: 'Maintenance due',
  LOW_STOCK: 'Low stock',
  PENDING_APPROVAL: 'Pending approval',
  SECURITY_ALERT: 'Security alert',
  ASSIGNMENT: 'Asset assigned to me',
  TRANSFER_DECISION: 'Transfer decided',
}

/**
 * US-NTF-05: per-event email on/off. In-app delivery has no switch by design
 * (US-NTF-03 - the always-available record), and Administrator-locked types
 * render visibly non-editable rather than hidden.
 */
export function NotificationPreferencesPage() {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const prefsQuery = useQuery({ queryKey: ['NTF', 'preferences'], queryFn: fetchNotificationPreferences })
  const update = useMutation({
    mutationFn: ({ eventType, emailEnabled }: { eventType: NotificationEventType; emailEnabled: boolean }) =>
      updateNotificationPreference(eventType, emailEnabled),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['NTF', 'preferences'] }),
    onError: (err) => setError(isApiProblem(err) ? err.detail : 'Failed to save the preference'),
  })

  if (prefsQuery.isLoading) return <LoadingSkeleton rows={6} />
  if (prefsQuery.isError) return <ErrorPanel error={prefsQuery.error} onRetry={() => prefsQuery.refetch()} />

  return (
    <Box>
      <PageHeader title="Notification Settings" />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 640 }}>
        Choose which events reach you by email. In-app notifications always deliver - they are your
        record even when email fails. Types the administrator has locked as mandatory cannot be turned off.
      </Typography>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      <Paper variant="outlined" sx={{ maxWidth: 640 }}>
        <List dense>
          {(prefsQuery.data ?? []).map((pref) => (
            <ListItem
              key={pref.eventType}
              divider
              secondaryAction={
                <Switch
                  edge="end"
                  checked={pref.emailEnabled}
                  disabled={pref.locked || update.isPending}
                  onChange={(e) =>
                    update.mutate({ eventType: pref.eventType, emailEnabled: e.target.checked })
                  }
                  slotProps={{ input: { 'aria-label': `Email for ${LABELS[pref.eventType]}` } }}
                />
              }
            >
              <ListItemText
                primary={
                  <Box component="span" sx={{ display: 'inline-flex', gap: 1, alignItems: 'center' }}>
                    {LABELS[pref.eventType] ?? pref.eventType}
                    {pref.locked && <Chip size="small" label="Mandatory" color="warning" variant="outlined" />}
                  </Box>
                }
                secondary={pref.locked ? 'Locked by the administrator' : 'Email'}
              />
            </ListItem>
          ))}
        </List>
      </Paper>
    </Box>
  )
}
