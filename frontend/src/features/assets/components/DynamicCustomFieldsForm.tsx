import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import FormControlLabel from '@mui/material/FormControlLabel'
import Checkbox from '@mui/material/Checkbox'
import Typography from '@mui/material/Typography'
import type { CustomFieldDefinition } from '../types'

interface DynamicCustomFieldsFormProps {
  definitions: CustomFieldDefinition[]
  values: Record<string, unknown>
  errors: Record<string, string>
  onChange: (fieldKey: string, value: unknown) => void
}

/**
 * Renders one input per category custom-field definition (FR-AST-06). The
 * field set is entirely data-driven - no category-specific code exists here
 * or anywhere else, matching the "no code changes for new custom fields"
 * requirement. Client-side shape mirrors buildCustomFieldsSchema exactly;
 * the server (CustomFieldValidationService) remains authoritative.
 */
export function DynamicCustomFieldsForm({ definitions, values, errors, onChange }: DynamicCustomFieldsFormProps) {
  if (definitions.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        This category has no custom fields configured.
      </Typography>
    )
  }

  return (
    <Grid container spacing={2}>
      {definitions.map((def) => {
        const value = values[def.fieldKey]
        const error = errors[def.fieldKey];

        return (
          <Grid size={{ xs: 12, sm: 6 }} key={def.id}>
            {def.dataType === 'BOOLEAN' ? (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={Boolean(value)}
                    onChange={(e) => onChange(def.fieldKey, e.target.checked)}
                  />
                }
                label={def.label}
              />
            ) : def.dataType === 'ENUM' ? (
              <TextField
                select
                fullWidth
                label={def.label}
                required={def.required}
                value={(value as string) ?? ''}
                error={!!error}
                helperText={error}
                onChange={(e) => onChange(def.fieldKey, e.target.value)}
              >
                {(def.enumOptions ?? []).map((option) => (
                  <MenuItem key={option} value={option}>
                    {option}
                  </MenuItem>
                ))}
              </TextField>
            ) : (
              <TextField
                fullWidth
                label={def.label}
                required={def.required}
                type={def.dataType === 'NUMBER' ? 'number' : def.dataType === 'DATE' ? 'date' : 'text'}
                slotProps={{ inputLabel: def.dataType === 'DATE' ? { shrink: true } : undefined }}
                value={(value as string | number) ?? ''}
                error={!!error}
                helperText={error}
                onChange={(e) => onChange(def.fieldKey, e.target.value)}
              />
            )}
          </Grid>
        )
      })}
    </Grid>
  )
}
