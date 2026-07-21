import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControlLabel from '@mui/material/FormControlLabel'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../api/errors'
import { usePasswordPolicyQuery, useUpdatePasswordPolicyMutation } from './hooks/usePasswordPolicyQuery'

/**
 * US-SEC-05: configure the password policy - the backend
 * (PasswordValidator/PasswordPolicyService/PasswordPolicyController) was
 * fully built with no admin screen to reach it; Super Admins had no way to
 * actually change the rules the system enforces.
 */
export function PasswordPolicySettingsPage() {
  const policyQuery = usePasswordPolicyQuery()
  const updatePolicy = useUpdatePasswordPolicyMutation()

  const [minLength, setMinLength] = useState(8)
  const [requireUppercase, setRequireUppercase] = useState(false)
  const [requireLowercase, setRequireLowercase] = useState(false)
  const [requireDigit, setRequireDigit] = useState(false)
  const [requireSpecial, setRequireSpecial] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (!policyQuery.data) return
    setMinLength(policyQuery.data.minLength)
    setRequireUppercase(policyQuery.data.requireUppercase)
    setRequireLowercase(policyQuery.data.requireLowercase)
    setRequireDigit(policyQuery.data.requireDigit)
    setRequireSpecial(policyQuery.data.requireSpecial)
  }, [policyQuery.data])

  async function handleSave() {
    if (!policyQuery.data) return
    setError(null)
    setSaved(false)
    try {
      await updatePolicy.mutateAsync({
        minLength,
        requireUppercase,
        requireLowercase,
        requireDigit,
        requireSpecial,
        version: policyQuery.data.version,
      })
      setSaved(true)
    } catch (err) {
      setError(isApiProblem(err) ? err.detail : 'Failed to save the password policy')
    }
  }

  return (
    <Box>
      <PageHeader title="Password Policy" />

      {policyQuery.isLoading && <LoadingSkeleton rows={4} />}
      {policyQuery.isError && <ErrorPanel error={policyQuery.error} onRetry={() => policyQuery.refetch()} />}

      {policyQuery.isSuccess && (
        <Paper variant="outlined" sx={{ p: 3, maxWidth: 480 }}>
          <Stack spacing={2}>
            {error && <Alert severity="error">{error}</Alert>}
            {saved && (
              <Alert severity="success" onClose={() => setSaved(false)}>
                Password policy saved.
              </Alert>
            )}
            <Typography variant="body2" color="text.secondary">
              Applies the next time any user sets or changes a password.
            </Typography>
            <TextField
              label="Minimum length"
              type="number"
              value={minLength}
              onChange={(e) => setMinLength(Number(e.target.value))}
              slotProps={{ htmlInput: { min: 1, max: 128 } }}
              sx={{ maxWidth: 220 }}
            />
            <FormControlLabel
              control={<Switch checked={requireUppercase} onChange={(e) => setRequireUppercase(e.target.checked)} />}
              label="Require an uppercase letter"
            />
            <FormControlLabel
              control={<Switch checked={requireLowercase} onChange={(e) => setRequireLowercase(e.target.checked)} />}
              label="Require a lowercase letter"
            />
            <FormControlLabel
              control={<Switch checked={requireDigit} onChange={(e) => setRequireDigit(e.target.checked)} />}
              label="Require a digit"
            />
            <FormControlLabel
              control={<Switch checked={requireSpecial} onChange={(e) => setRequireSpecial(e.target.checked)} />}
              label="Require a special character"
            />
            <Box>
              <Button variant="contained" onClick={handleSave} disabled={minLength < 1 || updatePolicy.isPending}>
                Save
              </Button>
            </Box>
          </Stack>
        </Paper>
      )}
    </Box>
  )
}
