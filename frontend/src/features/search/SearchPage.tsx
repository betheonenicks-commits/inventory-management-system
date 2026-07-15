import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Link from '@mui/material/Link'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import SearchIcon from '@mui/icons-material/Search'
import { PageHeader } from '../../components/common/PageHeader'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import { globalSearch, lookupAssetCode } from '../../api/search/searchApi'
import type { GlobalSearchResult } from '../../api/search/searchApi'

// An exact asset-code shape (AST-YYYY-NNNNNN) takes the US-SRC-02 fast path:
// resolve and jump straight to the asset, no results page in between.
const ASSET_CODE_PATTERN = /^AST-\d{4}-\d{6}$/i

/** EPIC-SRC: one box across assets, vendors, and people (US-SRC-01), with the scan/type code fast path (US-SRC-02). */
export function SearchPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const canRegister = hasPermission(user, 'assets:write')

  const [q, setQ] = useState('')
  const [result, setResult] = useState<GlobalSearchResult | null>(null)
  const [notFoundCode, setNotFoundCode] = useState<string | null>(null)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function runSearch() {
    const term = q.trim()
    if (!term) return
    setError(null)
    setNotFoundCode(null)
    setSearching(true)
    try {
      if (ASSET_CODE_PATTERN.test(term)) {
        try {
          const hit = await lookupAssetCode(term)
          navigate(`/assets/${hit.id}`)
          return
        } catch (err) {
          if (isApiProblem(err) && err.status === 404) {
            // AC-SRC-02-X: unrecognized code -> explicit not-found with a
            // "register this asset?" affordance for authorized roles.
            setNotFoundCode(term)
            setResult(null)
            return
          }
          throw err
        }
      }
      setResult(await globalSearch(term))
    } catch (err) {
      setResult(null)
      setError(isApiProblem(err) ? err.detail : 'Search failed')
    } finally {
      setSearching(false)
    }
  }

  const empty =
    result && result.assets.length === 0 && result.vendors.length === 0 && result.people.length === 0

  return (
    <Box>
      <PageHeader title="Search" />

      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={2}>
          <TextField
            fullWidth
            autoFocus
            label="Search assets, vendors, and people — or scan/type an asset code"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') runSearch()
            }}
          />
          <Button variant="contained" startIcon={<SearchIcon />} onClick={runSearch} disabled={searching}>
            Search
          </Button>
        </Stack>
      </Paper>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {notFoundCode && (
        <Alert
          severity="warning"
          action={
            canRegister && (
              <Button color="inherit" size="small" onClick={() => navigate('/assets/new')}>
                Register this asset
              </Button>
            )
          }
        >
          No asset matches the code "{notFoundCode}".
        </Alert>
      )}

      {searching && <LoadingSkeleton rows={4} />}

      {empty && <Alert severity="info">No matches for "{q.trim()}".</Alert>}

      {result && !empty && (
        <Stack spacing={2}>
          {result.assets.length > 0 && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Assets
              </Typography>
              <List dense disablePadding>
                {result.assets.map((a) => (
                  <ListItem key={a.id} disableGutters>
                    <ListItemText
                      primary={
                        <Link component={RouterLink} to={`/assets/${a.id}`} underline="hover">
                          {a.assetNumber} — {a.name}
                        </Link>
                      }
                      secondary={`${a.categoryName} · ${a.orgNodeName}${a.serialNumber ? ` · SN ${a.serialNumber}` : ''}`}
                    />
                    <Chip label={a.statusLabel} size="small" />
                  </ListItem>
                ))}
              </List>
            </Paper>
          )}

          {result.vendorsSearched && result.vendors.length > 0 && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                Vendors
              </Typography>
              <List dense disablePadding>
                {result.vendors.map((v) => (
                  <ListItem key={v.id} disableGutters>
                    <ListItemText
                      primary={
                        <Link component={RouterLink} to="/inventory/vendors" underline="hover">
                          {v.name}
                        </Link>
                      }
                    />
                    {!v.active && <Chip label="Inactive" size="small" />}
                  </ListItem>
                ))}
              </List>
            </Paper>
          )}

          {result.people.length > 0 && (
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                People
              </Typography>
              <List dense disablePadding>
                {result.people.map((p) => (
                  <ListItem key={p.id} disableGutters>
                    <ListItemText primary={p.fullName} secondary={p.orgNodeName ?? undefined} />
                  </ListItem>
                ))}
              </List>
            </Paper>
          )}
        </Stack>
      )}
    </Box>
  )
}
