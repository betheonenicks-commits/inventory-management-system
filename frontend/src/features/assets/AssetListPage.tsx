import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import AddIcon from '@mui/icons-material/Add'
import BookmarkAddIcon from '@mui/icons-material/BookmarkAdd'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { PageHeader } from '../../components/common/PageHeader'
import { EmptyState } from '../../components/common/EmptyState'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { AssetTable } from './components/AssetTable'
import { useAssetsQuery } from './hooks/useAssetsQuery'
import { useAssetCategoriesQuery } from './hooks/useAssetCategoriesQuery'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import {
  createSavedSearch,
  deleteSavedSearch,
  fetchSavedSearches,
  resolveSavedSearch,
} from '../../api/search/searchApi'

export function AssetListPage() {
  const navigate = useNavigate()
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'assets:write')
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchInput, setSearchInput] = useState(searchParams.get('q') ?? '')
  const debouncedQuery = useDebouncedValue(searchInput, 300)

  const page = Number(searchParams.get('page') ?? '0')
  const size = Number(searchParams.get('size') ?? '25')
  const categoryId = searchParams.get('categoryId') ?? undefined

  const filters = useMemo(
    () => ({ q: debouncedQuery || undefined, categoryId }),
    [debouncedQuery, categoryId],
  )

  const assetsQuery = useAssetsQuery(filters, page, size)
  const categoriesQuery = useAssetCategoriesQuery()

  // --- Saved searches (US-SRC-04) ---
  const queryClient = useQueryClient()
  const savedSearchesQuery = useQuery({ queryKey: ['SRC', 'savedSearches'], queryFn: fetchSavedSearches })
  const saveSearch = useMutation({
    mutationFn: createSavedSearch,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['SRC', 'savedSearches'] }),
  })
  const removeSearch = useMutation({
    mutationFn: deleteSavedSearch,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['SRC', 'savedSearches'] }),
  })
  const [saveOpen, setSaveOpen] = useState(false)
  const [saveName, setSaveName] = useState('')
  const [saveError, setSaveError] = useState<string | null>(null)
  const [appliedNotes, setAppliedNotes] = useState<string[]>([])

  async function handleSaveSearch() {
    setSaveError(null)
    try {
      await saveSearch.mutateAsync({
        name: saveName,
        query: debouncedQuery || undefined,
        categoryId: categoryId || undefined,
      })
      setSaveOpen(false)
      setSaveName('')
    } catch (err) {
      setSaveError(isApiProblem(err) ? err.detail : 'Failed to save this search')
    }
  }

  async function applySavedSearch(id: string) {
    setAppliedNotes([])
    try {
      // Resolve server-side so clauses referencing since-deleted entities are
      // dropped with a note instead of silently 404ing the filter (AC-SRC-04).
      const resolved = await resolveSavedSearch(id)
      setSearchInput(resolved.query ?? '')
      updateParams({ categoryId: resolved.categoryId ?? undefined, page: '0' })
      setAppliedNotes(resolved.droppedFilterNotes)
    } catch (err) {
      setAppliedNotes([isApiProblem(err) ? err.detail : 'Failed to apply the saved search'])
    }
  }

  function updateParams(next: Record<string, string | undefined>) {
    const params = new URLSearchParams(searchParams)
    Object.entries(next).forEach(([key, value]) => {
      if (value === undefined || value === '') {
        params.delete(key)
      } else {
        params.set(key, value)
      }
    })
    setSearchParams(params)
  }

  const hasFilters = !!debouncedQuery || !!categoryId

  return (
    <Box>
      <PageHeader
        title="Asset Register"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/assets/new')}>
              Register New Asset
            </Button>
          )
        }
      />

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField
          label="Search"
          size="small"
          value={searchInput}
          onChange={(e) => {
            setSearchInput(e.target.value)
            updateParams({ page: '0' })
          }}
          sx={{ minWidth: 260 }}
        />
        <TextField
          label="Category"
          size="small"
          select
          value={categoryId ?? ''}
          onChange={(e) => updateParams({ categoryId: e.target.value, page: '0' })}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">All categories</MenuItem>
          {(categoriesQuery.data ?? []).map((c) => (
            <MenuItem key={c.id} value={c.id}>
              {c.name}
            </MenuItem>
          ))}
        </TextField>
        <Button
          size="small"
          startIcon={<BookmarkAddIcon />}
          disabled={!hasFilters}
          onClick={() => {
            setSaveError(null)
            setSaveOpen(true)
          }}
        >
          Save search
        </Button>
      </Stack>

      {(savedSearchesQuery.data ?? []).length > 0 && (
        <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap', mb: 2 }}>
          {(savedSearchesQuery.data ?? []).map((s) => (
            <Chip
              key={s.id}
              label={s.name}
              onClick={() => applySavedSearch(s.id)}
              onDelete={() => removeSearch.mutate(s.id)}
              variant="outlined"
            />
          ))}
        </Stack>
      )}

      {appliedNotes.map((note) => (
        <Alert key={note} severity="info" sx={{ mb: 2 }} onClose={() => setAppliedNotes([])}>
          {note}
        </Alert>
      ))}

      <Dialog open={saveOpen} onClose={() => setSaveOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Save this search</DialogTitle>
        <DialogContent>
          {saveError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {saveError}
            </Alert>
          )}
          <TextField
            autoFocus
            fullWidth
            label="Name"
            value={saveName}
            onChange={(e) => setSaveName(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSaveOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveSearch} disabled={!saveName.trim() || saveSearch.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {assetsQuery.isLoading && <LoadingSkeleton rows={8} />}

      {assetsQuery.isError && <ErrorPanel error={assetsQuery.error} onRetry={() => assetsQuery.refetch()} />}

      {assetsQuery.isSuccess && assetsQuery.data.data.length === 0 && !hasFilters && (
        <EmptyState
          title="No assets registered yet"
          description="Get started by registering your first asset."
          action={
            canWrite ? (
              <Button variant="contained" onClick={() => navigate('/assets/new')}>
                Register your first asset
              </Button>
            ) : undefined
          }
        />
      )}

      {assetsQuery.isSuccess && assetsQuery.data.data.length === 0 && hasFilters && (
        <EmptyState
          title="No assets match these filters"
          action={
            <Button
              onClick={() => {
                setSearchInput('')
                updateParams({ q: undefined, categoryId: undefined, page: '0' })
              }}
            >
              Clear filters
            </Button>
          }
        />
      )}

      {assetsQuery.isSuccess && assetsQuery.data.data.length > 0 && (
        <AssetTable
          assets={assetsQuery.data.data}
          page={assetsQuery.data.page}
          onPageChange={(newPage) => updateParams({ page: String(newPage) })}
          onPageSizeChange={(newSize) => updateParams({ size: String(newSize), page: '0' })}
        />
      )}
    </Box>
  )
}
