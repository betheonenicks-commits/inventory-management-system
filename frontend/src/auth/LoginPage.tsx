import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import Alert from '@mui/material/Alert'
import { login } from '../api/authApi'
import { isApiProblem } from '../api/errors'
import { useAuthStore } from './authStore'
import { decodeJwtPayload } from './decodeJwt'

export function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const setSession = useAuthStore((s) => s.setSession)
  const navigate = useNavigate()
  const location = useLocation()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { accessToken, refreshToken } = await login(username, password)
      const claims = decodeJwtPayload(accessToken)
      setSession(accessToken, refreshToken, {
        username: claims?.username ?? username,
        roles: claims?.roles ?? [],
        permissions: claims?.permissions ?? [],
      })
      // US-NTF-10: a deep link that bounced through login lands on its exact
      // target afterwards, never a generic homepage. Only internal paths are
      // honored - an absolute URL in ?next= is ignored (open-redirect guard).
      const params = new URLSearchParams(location.search)
      const next = params.get('next')
      const from = (location.state as { from?: { pathname?: string; search?: string } } | null)?.from
      const target =
        next && next.startsWith('/') && !next.startsWith('//')
          ? next
          : from?.pathname
            ? `${from.pathname}${from.search ?? ''}`
            : '/assets'
      navigate(target, { replace: true })
    } catch (err) {
      if (isApiProblem(err)) {
        setError(err.detail || 'Invalid username or password')
      } else {
        setError('Unable to reach the server. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', bgcolor: 'grey.100' }}>
      <Paper elevation={3} sx={{ p: 4, width: 360 }} component="form" onSubmit={handleSubmit}>
        <Typography variant="h5" gutterBottom>
          IAMS Sign In
        </Typography>
        {error && (
          <Alert severity="error" sx={{ my: 2 }}>
            {error}
          </Alert>
        )}
        <TextField
          label="Username"
          fullWidth
          margin="normal"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoFocus
        />
        <TextField
          label="Password"
          type="password"
          fullWidth
          margin="normal"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Button type="submit" variant="contained" fullWidth sx={{ mt: 2 }} disabled={submitting}>
          {submitting ? 'Signing in...' : 'Sign In'}
        </Button>
      </Paper>
    </Box>
  )
}
