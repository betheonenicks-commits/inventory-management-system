import Alert from '@mui/material/Alert'
import AlertTitle from '@mui/material/AlertTitle'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import { isApiProblem } from '../../api/errors'

interface ErrorPanelProps {
  error: unknown
  onRetry?: () => void
}

// Every error surface distinguishes connectivity vs server error and, when
// the backend supplied one, shows the traceId for support reference - never
// a raw stack trace or blank panel (per the reconciled UX spec's error-state
// convention).
export function ErrorPanel({ error, onRetry }: ErrorPanelProps) {
  const problem = isApiProblem(error) ? error : null
  const message = problem?.detail ?? 'Something went wrong while contacting the server.'

  return (
    <Alert
      severity="error"
      action={
        onRetry && (
          <Button color="inherit" size="small" onClick={onRetry}>
            Try again
          </Button>
        )
      }
    >
      <AlertTitle>{problem?.title ?? 'Error'}</AlertTitle>
      {message}
      {problem?.traceId && (
        <Typography variant="caption" sx={{ display: 'block', mt: 1 }}>
          Reference: {problem.traceId}
        </Typography>
      )}
    </Alert>
  )
}
