import { useState } from 'react'
import Box from '@mui/material/Box'
import Chip from '@mui/material/Chip'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { EmptyState } from '../../components/common/EmptyState'
import { useExpiringLotsQuery } from './hooks/useInventoryStockQuery'

const WITHIN_DAYS_OPTIONS = [7, 14, 30, 60, 90]

function daysUntil(expiryDate: string): number {
  return Math.ceil((new Date(expiryDate).getTime() - Date.now()) / (24 * 60 * 60 * 1000))
}

/**
 * US-INV-09: lot/batch stock nearing expiry - the endpoint and its query
 * hook already existed (`useExpiringLotsQuery`), just never rendered by any
 * page anywhere, so a lot could silently expire unnoticed with no view to
 * catch it.
 */
export function ExpiringStockPage() {
  const [withinDays, setWithinDays] = useState(30)
  const expiringQuery = useExpiringLotsQuery(withinDays)

  return (
    <Box>
      <PageHeader title="Expiring Stock" />

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          select
          label="Within"
          size="small"
          value={withinDays}
          onChange={(e) => setWithinDays(Number(e.target.value))}
          sx={{ minWidth: 160 }}
        >
          {WITHIN_DAYS_OPTIONS.map((d) => (
            <MenuItem key={d} value={d}>
              {d} days
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {expiringQuery.isLoading && <LoadingSkeleton rows={5} />}
      {expiringQuery.isError && <ErrorPanel error={expiringQuery.error} onRetry={() => expiringQuery.refetch()} />}

      {expiringQuery.isSuccess && expiringQuery.data.length === 0 && (
        <EmptyState title={`Nothing expiring within ${withinDays} days`} />
      )}

      {expiringQuery.isSuccess && expiringQuery.data.length > 0 && (
        <Paper variant="outlined">
          <List>
            {expiringQuery.data.map((balance) => {
              const remaining = balance.expiryDate ? daysUntil(balance.expiryDate) : null
              return (
                <ListItem key={balance.id} divider>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Typography variant="body2">
                          {balance.itemName} ({balance.sku})
                        </Typography>
                        <Chip size="small" variant="outlined" label={`Lot ${balance.lotNumber}`} />
                        {remaining !== null && (
                          <Chip
                            size="small"
                            color={remaining <= 7 ? 'error' : remaining <= 14 ? 'warning' : 'default'}
                            label={remaining < 0 ? 'Expired' : `${remaining}d left`}
                          />
                        )}
                      </Stack>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary">
                        {balance.warehouseName} / {balance.subLocation} — {balance.quantityOnHand}{' '}
                        {balance.unitOfMeasure.toLowerCase()} on hand — expires{' '}
                        {balance.expiryDate ? new Date(balance.expiryDate).toLocaleDateString() : 'unknown'}
                      </Typography>
                    }
                  />
                </ListItem>
              )
            })}
          </List>
        </Paper>
      )}
    </Box>
  )
}
