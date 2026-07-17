import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Snackbar from '@mui/material/Snackbar'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../api/errors'
import { submitFeedback } from '../../api/analytics/analyticsApi'
import type { FeedbackCategory } from '../../api/analytics/analyticsApi'

const CATEGORIES: { value: FeedbackCategory; label: string }[] = [
  { value: 'BUG', label: 'Something is broken' },
  { value: 'FRICTION', label: 'Something is awkward or slow' },
  { value: 'IDEA', label: 'An idea or suggestion' },
  { value: 'PRAISE', label: 'Something works well' },
  { value: 'OTHER', label: 'Other' },
]

/**
 * US-ANL-04: category + optional free text, from wherever the user is (the
 * current route goes along as context). Text is optional by design -
 * category alone is accepted. The snackbar is the receipt confirmation.
 */
export function FeedbackDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const location = useLocation()
  const [category, setCategory] = useState<FeedbackCategory>('FRICTION')
  const [message, setMessage] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [confirmed, setConfirmed] = useState(false)

  async function send() {
    setError(null)
    setSending(true)
    try {
      await submitFeedback(category, message, location.pathname)
      setMessage('')
      onClose()
      setConfirmed(true)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to send feedback')
    } finally {
      setSending(false)
    }
  }

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>Send feedback</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Goes straight to your administrators - no separate email or ticket needed. Leaving the text empty is
              fine; the category alone is useful signal.
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            <FormControl>
              <InputLabel id="feedback-category-label">Category</InputLabel>
              <Select
                labelId="feedback-category-label"
                label="Category"
                value={category}
                onChange={(e) => setCategory(e.target.value as FeedbackCategory)}
              >
                {CATEGORIES.map((c) => (
                  <MenuItem key={c.value} value={c.value}>
                    {c.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="What happened? (optional)"
              fullWidth
              multiline
              minRows={3}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              slotProps={{ htmlInput: { maxLength: 2000 } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button variant="contained" onClick={send} disabled={sending}>
            Send feedback
          </Button>
        </DialogActions>
      </Dialog>
      <Snackbar
        open={confirmed}
        autoHideDuration={5000}
        onClose={() => setConfirmed(false)}
        message="Thanks - your feedback was received and routed to the administrators."
      />
    </>
  )
}
