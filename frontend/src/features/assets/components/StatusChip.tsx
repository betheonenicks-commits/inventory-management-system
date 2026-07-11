import Chip from '@mui/material/Chip'
import type { AssetStatus } from '../types'

const COLOR_BY_CODE: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
  IN_USE: 'success',
  IN_STORAGE: 'info',
  UNDER_REPAIR: 'warning',
  MISSING: 'error',
  RETIRED: 'default',
  DISPOSED: 'default',
  VOID: 'default',
}

export function StatusChip({ status }: { status: AssetStatus }) {
  return <Chip size="small" label={status.label} color={COLOR_BY_CODE[status.code] ?? 'default'} />
}
