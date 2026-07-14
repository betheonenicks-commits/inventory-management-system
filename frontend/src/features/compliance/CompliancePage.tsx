import { useState } from 'react'
import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import { PageHeader } from '../../components/common/PageHeader'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { RetentionPolicyPanel } from './components/RetentionPolicyPanel'
import { LegalHoldsPanel } from './components/LegalHoldsPanel'
import { PersonAnonymizationPanel } from './components/PersonAnonymizationPanel'
import { PrivacyNoticesPanel } from './components/PrivacyNoticesPanel'
import { AccessibilityAuditPanel } from './components/AccessibilityAuditPanel'
import { DataResidencyPanel } from './components/DataResidencyPanel'

const TABS = [
  { label: 'Retention Policies', component: RetentionPolicyPanel },
  { label: 'Legal Holds', component: LegalHoldsPanel },
  { label: 'Anonymization', component: PersonAnonymizationPanel },
  { label: 'Privacy Notices', component: PrivacyNoticesPanel },
  { label: 'Accessibility', component: AccessibilityAuditPanel },
  { label: 'Data Residency', component: DataResidencyPanel },
]

/** EPIC-CMP: Compliance & Data Privacy - retention, legal holds, anonymization, privacy notices, accessibility, data residency. */
export function CompliancePage() {
  const [tab, setTab] = useState(0)
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'compliance:write')
  const ActivePanel = TABS[tab].component

  return (
    <Box>
      <PageHeader title="Compliance" />

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }} variant="scrollable" scrollButtons="auto">
        {TABS.map((t) => (
          <Tab key={t.label} label={t.label} />
        ))}
      </Tabs>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <ActivePanel canWrite={canWrite} />
      </Paper>
    </Box>
  )
}
