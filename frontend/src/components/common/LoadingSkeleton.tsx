import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'

// Skeleton loaders, not spinners, for row/field-shaped content per the UX
// spec's loading-state convention - spinners are reserved for button
// in-flight state or unknowable-shape full-page boot.
export function LoadingSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <Stack spacing={1}>
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} variant="rounded" height={40} />
      ))}
    </Stack>
  )
}
