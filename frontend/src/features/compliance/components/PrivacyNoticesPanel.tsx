import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import { ErrorPanel } from '../../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../../api/errors'
import {
  useDeletePrivacyNoticeMutation,
  usePrivacyNoticesQuery,
  useSavePrivacyNoticeMutation,
} from '../hooks/usePrivacyNoticesQuery'

/** US-CMP-03: privacy-notice text per personal-data field, shown to any authenticated user at the point of capture. */
export function PrivacyNoticesPanel({ canWrite }: { canWrite: boolean }) {
  const noticesQuery = usePrivacyNoticesQuery()
  const saveNotice = useSavePrivacyNoticeMutation()
  const deleteNotice = useDeletePrivacyNoticeMutation()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [fieldName, setFieldName] = useState('')
  const [noticeText, setNoticeText] = useState('')
  const [error, setError] = useState<string | null>(null)

  function openDialog() {
    setFieldName('')
    setNoticeText('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleSave() {
    setError(null)
    try {
      await saveNotice.mutateAsync({ fieldName, noticeText })
      setDialogOpen(false)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save privacy notice')
    }
  }

  if (noticesQuery.isLoading) return <LoadingSkeleton rows={3} />
  if (noticesQuery.isError) return <ErrorPanel error={noticesQuery.error} onRetry={() => noticesQuery.refetch()} />

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="subtitle1">Privacy Notices</Typography>
        {canWrite && (
          <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={openDialog}>
            New Notice
          </Button>
        )}
      </Stack>

      <List dense>
        {(noticesQuery.data ?? []).map((notice) => (
          <ListItem
            key={notice.id}
            divider
            secondaryAction={
              canWrite && (
                <Button size="small" color="error" onClick={() => deleteNotice.mutate(notice.id)}>
                  Delete
                </Button>
              )
            }
          >
            <ListItemText primary={notice.fieldName} secondary={notice.noticeText} />
          </ListItem>
        ))}
        {(noticesQuery.data ?? []).length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No privacy notices configured yet.
          </Typography>
        )}
      </List>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>New Privacy Notice</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Field Name"
              required
              fullWidth
              helperText="e.g. phone, email"
              value={fieldName}
              onChange={(e) => setFieldName(e.target.value)}
            />
            <TextField
              label="Notice Text"
              required
              fullWidth
              multiline
              minRows={3}
              value={noticeText}
              onChange={(e) => setNoticeText(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSave} disabled={!fieldName || !noticeText || saveNotice.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
