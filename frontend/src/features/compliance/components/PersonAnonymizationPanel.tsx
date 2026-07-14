import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../../api/errors'
import type { ApiProblem } from '../../../api/errors'
import { useAnonymizationEligibleQuery, useAnonymizePersonMutation } from '../hooks/usePersonAnonymizationQuery'
import type { PersonAnonymization } from '../types'

/** US-CMP-02 / US-LIF-14: departed persons eligible for anonymization, approved explicitly one at a time. */
export function PersonAnonymizationPanel({ canWrite }: { canWrite: boolean }) {
  const eligibleQuery = useAnonymizationEligibleQuery()
  const anonymize = useAnonymizePersonMutation()
  const [target, setTarget] = useState<PersonAnonymization | null>(null)
  const [blockedProblem, setBlockedProblem] = useState<ApiProblem | null>(null)

  async function handleConfirm() {
    if (!target) return
    setBlockedProblem(null)
    try {
      await anonymize.mutateAsync(target.id)
      setTarget(null)
    } catch (err) {
      if (isApiProblem(err)) {
        setBlockedProblem(err)
      } else {
        setTarget(null)
      }
    }
  }

  if (eligibleQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (eligibleQuery.isError) return <ErrorPanel error={eligibleQuery.error} onRetry={() => eligibleQuery.refetch()} />

  return (
    <Stack spacing={2}>
      <Typography variant="subtitle1">Anonymization-Eligible Persons</Typography>
      <Typography variant="body2" color="text.secondary">
        Departed (inactive) persons not yet anonymized. Anonymizing is deliberate, not automatic - it is blocked while
        any asset is still assigned to the person.
      </Typography>

      <List dense>
        {(eligibleQuery.data ?? []).map((person) => (
          <ListItem
            key={person.id}
            divider
            secondaryAction={
              canWrite && (
                <Button size="small" color="error" onClick={() => setTarget(person)}>
                  Anonymize
                </Button>
              )
            }
          >
            <ListItemText primary={person.fullName} />
          </ListItem>
        ))}
        {(eligibleQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No eligible persons - nothing to anonymize right now.
          </Typography>
        )}
      </List>

      <Dialog open={!!target} onClose={() => setTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Anonymize {target?.fullName}?</DialogTitle>
        <DialogContent>
          {blockedProblem ? (
            <Alert severity="warning">
              <Typography variant="subtitle2">{blockedProblem.title}</Typography>
              {blockedProblem.detail}
              {blockedProblem.blockingAssets && blockedProblem.blockingAssets.length > 0 && (
                <List dense sx={{ mt: 1 }}>
                  {blockedProblem.blockingAssets.map((asset) => (
                    <ListItem key={asset.assetId} disableGutters>
                      <ListItemText primary={`${asset.assetNumber} — ${asset.name}`} />
                    </ListItem>
                  ))}
                </List>
              )}
            </Alert>
          ) : (
            <DialogContentText>
              This permanently replaces this person&apos;s name and email with an anonymized placeholder. Their
              record id is preserved, so historical audit findings and asset history remain intact. This cannot be
              undone.
            </DialogContentText>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTarget(null)}>{blockedProblem ? 'Close' : 'Cancel'}</Button>
          {!blockedProblem && (
            <Button color="error" variant="contained" onClick={handleConfirm} disabled={anonymize.isPending}>
              Anonymize
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
