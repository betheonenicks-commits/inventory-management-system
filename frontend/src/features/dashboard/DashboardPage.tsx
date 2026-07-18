import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import FormControlLabel from '@mui/material/FormControlLabel'
import FormGroup from '@mui/material/FormGroup'
import LinearProgress from '@mui/material/LinearProgress'
import Link from '@mui/material/Link'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import TuneIcon from '@mui/icons-material/Tune'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { isApiProblem } from '../../api/errors'
import {
  useActivityFeedQuery,
  useAssetSummaryQuery,
  useAuditCalendarQuery,
  useAuditCompletionQuery,
  useDashboardLowStockQuery,
  useDashboardPreferencesQuery,
  useExpirationsQuery,
  useSaveDashboardPreferencesMutation,
} from './hooks/useDashboardQueries'
import type { DashboardTile, ExpirationKind } from './types'

const TILE_LABELS: Record<DashboardTile, string> = {
  ASSET_SUMMARY: 'Asset Summary',
  AUDIT_COMPLETION: 'Audit Completion',
  EXPIRATIONS: 'Upcoming Expirations & Maintenance',
  LOW_STOCK: 'Low Stock',
  ACTIVITY_FEED: 'Recent Activity',
  AUDIT_CALENDAR: 'Audit Calendar',
}

const EXPIRATION_KIND_COLOR: Record<ExpirationKind, 'info' | 'secondary' | 'warning'> = {
  WARRANTY: 'info',
  INSURANCE: 'secondary',
  MAINTENANCE: 'warning',
}

/**
 * EPIC-DSH: role- and scope-filtered KPI dashboard (US-DSH-01..07). Which
 * tiles render (and in what order) comes from the user's saved preference -
 * the backend returns the full default set until they configure one
 * (US-DSH-06). Every figure is already org-scoped server-side (US-DSH-07);
 * nothing here filters client-side.
 */
export function DashboardPage() {
  const prefsQuery = useDashboardPreferencesQuery()
  const savePrefs = useSaveDashboardPreferencesMutation()

  const tiles = prefsQuery.data?.tiles ?? []
  const enabled = (tile: DashboardTile) => prefsQuery.isSuccess && tiles.includes(tile)

  const assetSummary = useAssetSummaryQuery(enabled('ASSET_SUMMARY'))
  const auditCompletion = useAuditCompletionQuery(enabled('AUDIT_COMPLETION'))
  const expirations = useExpirationsQuery(enabled('EXPIRATIONS'))
  const lowStock = useDashboardLowStockQuery(enabled('LOW_STOCK'))
  const activityFeed = useActivityFeedQuery(enabled('ACTIVITY_FEED'))
  const auditCalendar = useAuditCalendarQuery(enabled('AUDIT_CALENDAR'))

  const [configOpen, setConfigOpen] = useState(false)
  const [draftTiles, setDraftTiles] = useState<DashboardTile[]>([])
  const [configError, setConfigError] = useState<string | null>(null)

  function openConfig() {
    setDraftTiles(tiles)
    setConfigError(null)
    setConfigOpen(true)
  }

  function toggleDraftTile(tile: DashboardTile) {
    // Preserve the canonical availableTiles order rather than click order, so
    // re-adding a tile puts it back where users expect it.
    setDraftTiles((prev) => {
      const all = prefsQuery.data?.availableTiles ?? []
      const next = prev.includes(tile) ? prev.filter((t) => t !== tile) : [...prev, tile]
      return all.filter((t) => next.includes(t))
    })
  }

  async function handleSaveConfig() {
    setConfigError(null)
    try {
      await savePrefs.mutateAsync(draftTiles)
      setConfigOpen(false)
    } catch (err) {
      setConfigError(isApiProblem(err) ? err.detail : 'Failed to save dashboard configuration')
    }
  }

  return (
    <Box>
      <PageHeader
        title="Dashboard"
        actions={
          <Button variant="outlined" startIcon={<TuneIcon />} onClick={openConfig} disabled={!prefsQuery.isSuccess}>
            Configure
          </Button>
        }
      />

      {prefsQuery.isLoading && <LoadingSkeleton rows={4} />}
      {prefsQuery.isError && <ErrorPanel error={prefsQuery.error} onRetry={() => prefsQuery.refetch()} />}

      {prefsQuery.isSuccess && tiles.length === 0 && (
        <Alert severity="info">
          No tiles are configured. Use Configure to choose which KPIs appear on your dashboard.
        </Alert>
      )}

      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
        {tiles.map((tile) => (
          <DashboardTileFrame key={tile} title={TILE_LABELS[tile]}>
            {tile === 'ASSET_SUMMARY' && (
              <QueryStates query={assetSummary}>
                {(data) => (
                  <Stack spacing={1.5}>
                    <Typography variant="h3" component="div">
                      {data.totalAssets}
                      <Typography component="span" variant="body1" color="text.secondary" sx={{ ml: 1 }}>
                        total assets in your scope
                      </Typography>
                    </Typography>
                    <Divider />
                    <Typography variant="subtitle2">By status</Typography>
                    <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                      {data.byStatus.map((s) => (
                        <Chip key={s.label} label={`${s.label}: ${s.count}`} size="small" />
                      ))}
                      {data.byStatus.length === 0 && <Typography color="text.secondary">No assets</Typography>}
                    </Stack>
                    <Typography variant="subtitle2">By category</Typography>
                    <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                      {data.byCategory.map((c) => (
                        <Chip key={c.label} label={`${c.label}: ${c.count}`} size="small" variant="outlined" />
                      ))}
                      {data.byCategory.length === 0 && <Typography color="text.secondary">No assets</Typography>}
                    </Stack>
                  </Stack>
                )}
              </QueryStates>
            )}

            {tile === 'AUDIT_COMPLETION' && (
              <QueryStates query={auditCompletion}>
                {(data) =>
                  data.averagePercentComplete === null && data.recentlyClosed.length === 0 ? (
                    <Typography color="text.secondary">No audits in your scope.</Typography>
                  ) : (
                    <Stack spacing={1.5}>
                      {data.averagePercentComplete !== null && (
                        <>
                          <Typography variant="h3" component="div">
                            {data.averagePercentComplete}%
                            <Typography component="span" variant="body1" color="text.secondary" sx={{ ml: 1 }}>
                              average completion across {data.audits.length} active audit
                              {data.audits.length === 1 ? '' : 's'}
                            </Typography>
                          </Typography>
                          <List dense disablePadding>
                            {data.audits.slice(0, 6).map((a) => (
                              <ListItem key={a.auditId} disableGutters>
                                <ListItemText
                                  primary={
                                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                                      <Link component={RouterLink} to={`/audits/${a.auditId}`} underline="hover">
                                        {a.name}
                                      </Link>
                                      {a.status === 'PENDING_APPROVAL' && (
                                        <Chip size="small" color="warning" label="Pending approval" />
                                      )}
                                      {a.exceptionCount > 0 && (
                                        <Chip
                                          size="small"
                                          color="error"
                                          variant="outlined"
                                          label={`${a.exceptionCount} exception${a.exceptionCount === 1 ? '' : 's'}`}
                                        />
                                      )}
                                    </Stack>
                                  }
                                  secondary={
                                    <LinearProgress variant="determinate" value={a.percentComplete} sx={{ mt: 0.5 }} />
                                  }
                                  slotProps={{ secondary: { component: 'div' } }}
                                />
                                <Typography variant="body2" sx={{ ml: 2, minWidth: 40, textAlign: 'right' }}>
                                  {a.percentComplete}%
                                </Typography>
                              </ListItem>
                            ))}
                          </List>
                        </>
                      )}

                      {data.recentlyClosed.length > 0 && (
                        <>
                          <Divider textAlign="left">
                            <Typography variant="caption" color="text.secondary">
                              Recently closed
                            </Typography>
                          </Divider>
                          <List dense disablePadding>
                            {data.recentlyClosed.map((a) => (
                              <ListItem key={a.auditId} disableGutters>
                                <ListItemText
                                  primary={
                                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                                      <Link component={RouterLink} to={`/audits/${a.auditId}`} underline="hover">
                                        {a.name}
                                      </Link>
                                      {a.exceptionCount > 0 && (
                                        <Chip
                                          size="small"
                                          color="error"
                                          variant="outlined"
                                          label={`${a.exceptionCount} exception${a.exceptionCount === 1 ? '' : 's'}`}
                                        />
                                      )}
                                    </Stack>
                                  }
                                />
                                <Chip size="small" color="success" variant="outlined" label="Closed" />
                              </ListItem>
                            ))}
                          </List>
                        </>
                      )}
                    </Stack>
                  )
                }
              </QueryStates>
            )}

            {tile === 'EXPIRATIONS' && (
              <QueryStates query={expirations}>
                {(data) =>
                  data.length === 0 ? (
                    <Typography color="text.secondary">Nothing due in the next 30 days.</Typography>
                  ) : (
                    <List dense disablePadding>
                      {data.slice(0, 8).map((e, i) => (
                        <ListItem key={`${e.kind}-${e.assetId}-${i}`} disableGutters>
                          <Chip label={e.kind} size="small" color={EXPIRATION_KIND_COLOR[e.kind]} sx={{ mr: 1.5, minWidth: 110 }} />
                          <ListItemText
                            primary={
                              <Link component={RouterLink} to={`/assets/${e.assetId}`} underline="hover">
                                {e.assetName}
                              </Link>
                            }
                            secondary={e.detail ?? undefined}
                          />
                          <Typography variant="body2" color="text.secondary">
                            {e.dueDate}
                          </Typography>
                        </ListItem>
                      ))}
                    </List>
                  )
                }
              </QueryStates>
            )}

            {tile === 'LOW_STOCK' && (
              <QueryStates query={lowStock}>
                {(data) =>
                  data.length === 0 ? (
                    <Typography color="text.secondary">All items are at or above their reorder level.</Typography>
                  ) : (
                    <List dense disablePadding>
                      {data.map((item) => (
                        <ListItem key={item.itemId} disableGutters>
                          <ListItemText
                            primary={
                              <Link component={RouterLink} to={`/inventory/items/${item.itemId}`} underline="hover">
                                {item.name}
                              </Link>
                            }
                            secondary={item.sku}
                          />
                          <Chip
                            label={`${item.totalQuantity} / reorder at ${item.reorderLevel} ${item.unitOfMeasure}`}
                            size="small"
                            color="warning"
                          />
                        </ListItem>
                      ))}
                    </List>
                  )
                }
              </QueryStates>
            )}

            {tile === 'ACTIVITY_FEED' && (
              <QueryStates query={activityFeed}>
                {(data) =>
                  data.length === 0 ? (
                    <Typography color="text.secondary">No recent activity in your scope.</Typography>
                  ) : (
                    <List dense disablePadding>
                      {data.slice(0, 10).map((e) => (
                        <ListItem key={e.eventId} disableGutters>
                          <ListItemText
                            primary={
                              <>
                                <Chip label={e.eventType.replaceAll('_', ' ')} size="small" sx={{ mr: 1 }} />
                                <Link component={RouterLink} to={`/assets/${e.assetId}`} underline="hover">
                                  {e.assetName}
                                </Link>
                              </>
                            }
                            secondary={new Date(e.occurredAt).toLocaleString()}
                          />
                        </ListItem>
                      ))}
                    </List>
                  )
                }
              </QueryStates>
            )}

            {tile === 'AUDIT_CALENDAR' && (
              <QueryStates query={auditCalendar}>
                {(data) =>
                  data.length === 0 ? (
                    <Typography color="text.secondary">No audits scheduled in the next 30 days.</Typography>
                  ) : (
                    <List dense disablePadding>
                      {data.map((a) => (
                        <ListItem key={a.auditId} disableGutters>
                          <Chip label={a.scheduledDate} size="small" sx={{ mr: 1.5 }} />
                          <ListItemText
                            primary={
                              <Link component={RouterLink} to={`/audits/${a.auditId}`} underline="hover">
                                {a.name}
                              </Link>
                            }
                            secondary={a.status.replaceAll('_', ' ')}
                          />
                        </ListItem>
                      ))}
                    </List>
                  )
                }
              </QueryStates>
            )}
          </DashboardTileFrame>
        ))}
      </Box>

      <Dialog open={configOpen} onClose={() => setConfigOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Configure dashboard</DialogTitle>
        <DialogContent>
          {configError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {configError}
            </Alert>
          )}
          <FormGroup>
            {(prefsQuery.data?.availableTiles ?? []).map((tile) => (
              <FormControlLabel
                key={tile}
                control={<Checkbox checked={draftTiles.includes(tile)} onChange={() => toggleDraftTile(tile)} />}
                label={TILE_LABELS[tile]}
              />
            ))}
          </FormGroup>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfigOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveConfig} disabled={savePrefs.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

function DashboardTileFrame({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        {title}
      </Typography>
      {children}
    </Paper>
  )
}

interface QueryLike<T> {
  isLoading: boolean
  isError: boolean
  isSuccess: boolean
  data: T | undefined
  error: unknown
  refetch: () => void
}

function QueryStates<T>({ query, children }: { query: QueryLike<T>; children: (data: T) => ReactNode }) {
  if (query.isLoading) return <LoadingSkeleton rows={3} />
  if (query.isError) return <ErrorPanel error={query.error} onRetry={() => query.refetch()} />
  if (query.isSuccess && query.data !== undefined) return <>{children(query.data)}</>
  return null
}
