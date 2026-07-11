import Box from '@mui/material/Box'
import Divider from '@mui/material/Divider'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import CircleIcon from '@mui/icons-material/Circle'
import type { AssetHistoryEvent } from '../types'

const EVENT_LABEL: Record<string, string> = {
  STATUS_CHANGE: 'Status changed',
  LOCATION_CHANGE: 'Location changed',
  ASSIGNMENT_CHANGE: 'Assignment changed',
  CONDITION_CHANGE: 'Condition changed',
  FIELD_UPDATE: 'Field updated',
  LIFECYCLE_EVENT: 'Registered',
  CORRECTION: 'Correction',
}

// Append-only per FR-AST-10 - this timeline is read-only by construction:
// there is no edit/delete affordance anywhere in this component, because
// the backend never exposes a mutating endpoint for history rows.
export function AssetHistoryTimeline({ events }: { events: AssetHistoryEvent[] }) {
  if (events.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        No history yet.
      </Typography>
    )
  }

  return (
    <Stack spacing={0}>
      {events.map((event, index) => (
        <Box key={event.id}>
          <Stack direction="row" spacing={1.5} sx={{ py: 1.5, alignItems: 'flex-start' }}>
            <CircleIcon sx={{ fontSize: 10, mt: 0.7, color: 'primary.main', flexShrink: 0 }} />
            <Box sx={{ flexGrow: 1 }}>
              <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {EVENT_LABEL[event.eventType] ?? event.eventType}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {new Date(event.createdAt).toLocaleString()}
                </Typography>
              </Stack>
              {event.fieldName && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  {event.fieldName}: {event.oldValue ?? '—'} &rarr; {event.newValue ?? '—'}
                </Typography>
              )}
            </Box>
          </Stack>
          {index < events.length - 1 && <Divider />}
        </Box>
      ))}
    </Stack>
  )
}
