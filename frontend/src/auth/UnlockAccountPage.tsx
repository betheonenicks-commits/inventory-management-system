import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import Alert from '@mui/material/Alert'
import Link from '@mui/material/Link'
import Stack from '@mui/material/Stack'
import { confirmUnlock, requestUnlock } from '../api/authApi'
import { isApiProblem } from '../api/errors'

/**
 * US-SEC-09's self-service half: a locked-out user requests an emailed code,
 * then redeems it here. Step 1 always "succeeds" from the caller's point of
 * view - the backend never reveals whether the username exists or was even
 * locked, so there is nothing here to distinguish it on either.
 */
export function UnlockAccountPage() {
  const [username, setUsername] = useState('')
  const [requestSubmitting, setRequestSubmitting] = useState(false)
  const [requested, setRequested] = useState(false)

  const [code, setCode] = useState('')
  const [confirmError, setConfirmError] = useState<string | null>(null)
  const [confirmSubmitting, setConfirmSubmitting] = useState(false)
  const [unlocked, setUnlocked] = useState(false)
  const navigate = useNavigate()

  async function handleRequest(e: React.FormEvent) {
    e.preventDefault()
    setRequestSubmitting(true)
    try {
      await requestUnlock(username)
    } finally {
      setRequestSubmitting(false)
      setRequested(true)
    }
  }

  async function handleConfirm(e: React.FormEvent) {
    e.preventDefault()
    setConfirmError(null)
    setConfirmSubmitting(true)
    try {
      await confirmUnlock(code.trim())
      setUnlocked(true)
    } catch (err) {
      setConfirmError(isApiProblem(err) ? err.detail : 'Unable to reach the server. Please try again.')
    } finally {
      setConfirmSubmitting(false)
    }
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', bgcolor: 'grey.100' }}>
      <Paper elevation={3} sx={{ p: 4, width: 400 }}>
        <Typography variant="h5" gutterBottom>
          Unlock Your Account
        </Typography>

        {unlocked ? (
          <Stack spacing={2}>
            <Alert severity="success">Your account is unlocked. You can sign in now.</Alert>
            <Button variant="contained" fullWidth onClick={() => navigate('/login', { replace: true })}>
              Go to Sign In
            </Button>
          </Stack>
        ) : (
          <Stack spacing={3}>
            <Box component="form" onSubmit={handleRequest}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                1. Enter your username. If your account is locked, we&apos;ll email an unlock code.
              </Typography>
              <TextField
                label="Username"
                fullWidth
                margin="dense"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoFocus
              />
              <Button type="submit" variant="outlined" fullWidth sx={{ mt: 1 }} disabled={requestSubmitting || !username}>
                {requestSubmitting ? 'Sending...' : 'Send Unlock Code'}
              </Button>
              {requested && (
                <Alert severity="info" sx={{ mt: 2 }}>
                  If that account is locked, an unlock code has been emailed to it.
                </Alert>
              )}
            </Box>

            <Box component="form" onSubmit={handleConfirm}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                2. Enter the code from the email to unlock your account.
              </Typography>
              {confirmError && (
                <Alert severity="error" sx={{ mb: 1 }}>
                  {confirmError}
                </Alert>
              )}
              <TextField
                label="Unlock Code"
                fullWidth
                margin="dense"
                value={code}
                onChange={(e) => setCode(e.target.value)}
              />
              <Button type="submit" variant="contained" fullWidth sx={{ mt: 1 }} disabled={confirmSubmitting || !code}>
                {confirmSubmitting ? 'Unlocking...' : 'Unlock Account'}
              </Button>
            </Box>

            <Link component={RouterLink} to="/login" variant="body2" sx={{ textAlign: 'center' }}>
              Back to Sign In
            </Link>
          </Stack>
        )}
      </Paper>
    </Box>
  )
}
