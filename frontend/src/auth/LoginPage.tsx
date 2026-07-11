import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const setSession = useAuthStore((s) => s.setSession)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { accessToken } = await login(username, password)
      const claims = decodeJwtPayload(accessToken)
      setSession(accessToken, { username: claims?.username ?? username, roles: claims?.roles ?? [] })
      navigate('/assets', { replace: true })
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
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Development mode - single administrator account
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
