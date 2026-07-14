import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import AddIcon from '@mui/icons-material/Add'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { PageHeader } from '../../components/common/PageHeader'
import { EmptyState } from '../../components/common/EmptyState'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { AssetTable } from './components/AssetTable'
import { useAssetsQuery } from './hooks/useAssetsQuery'
import { useAssetCategoriesQuery } from './hooks/useAssetCategoriesQuery'
import { useAuthStore, hasPermission } from '../../auth/authStore'

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
      </Stack>

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
