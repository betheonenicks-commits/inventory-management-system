import Typography from '@mui/material/Typography'
import { usePrivacyNoticesQuery } from '../../features/compliance/hooks/usePrivacyNoticesQuery'

/**
 * US-CMP-03: renders the admin-configured privacy notice for a given field
 * (see PrivacyNoticesPanel.tsx, Compliance section) alongside the field it
 * describes on an actual data-capture form - the notice text was previously
 * only ever visible in the Compliance admin panel itself, never here. `GET
 * /compliance/privacy-notices` has no permission gate, so any authenticated
 * user filling out the form sees it. Renders nothing if no notice is
 * configured for this field name (US-CMP-03's second clause: unconfigured =
 * no forced complexity).
 */
export function PrivacyNoticeText({ fieldName }: { fieldName: string }) {
  const noticesQuery = usePrivacyNoticesQuery()
  const notice = (noticesQuery.data ?? []).find((n) => n.fieldName.toLowerCase() === fieldName.toLowerCase())
  if (!notice) return null
  return (
    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: -1 }}>
      {notice.noticeText}
    </Typography>
  )
}
