import Alert from '@mui/material/Alert'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { Asset } from '../types'
import type { ChildDisposition } from '../../lifecycle/types'

/**
 * US-AST-04: when a parent asset is transferred or disposed, each component (child)
 * asset must be explicitly dispositioned before the action can be requested. The
 * backend blocks a request that omits any child; this prompts for each one so the
 * user resolves them up front. "Block" is expressed by simply not choosing - the
 * caller keeps the submit button disabled until every child has a disposition.
 */
export function ChildDispositionFields({
  childAssets,
  value,
  onChange,
  verb,
}: {
  childAssets: Asset[]
  value: Record<string, ChildDisposition>
  onChange: (next: Record<string, ChildDisposition>) => void
  verb: string
}) {
  if (childAssets.length === 0) return null

  return (
    <Stack spacing={1}>
      <Typography variant="subtitle2">Component assets</Typography>
      <Alert severity="info" sx={{ py: 0.5 }}>
        This asset has component assets. Disposition each one before you {verb} the parent.
      </Alert>
      {childAssets.map((child) => (
        <FormControl key={child.id} fullWidth size="small">
          <InputLabel id={`child-disp-${child.id}`}>
            {child.assetNumber} — {child.name}
          </InputLabel>
          <Select
            labelId={`child-disp-${child.id}`}
            label={`${child.assetNumber} — ${child.name}`}
            value={value[child.id] ?? ''}
            onChange={(e) => onChange({ ...value, [child.id]: e.target.value as ChildDisposition })}
          >
            <MenuItem value="MOVE_WITH_PARENT">Move with parent</MenuItem>
            <MenuItem value="DETACH">Detach (leave as standalone)</MenuItem>
          </Select>
        </FormControl>
      ))}
    </Stack>
  )
}
