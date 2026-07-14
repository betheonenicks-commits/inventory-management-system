import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Link from '@mui/material/Link'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { useAssetQuery } from '../hooks/useAssetQuery'
import { useAssetsQuery } from '../hooks/useAssetsQuery'
import { useAssetChildrenQuery, useLinkChildMutation, useUnlinkChildMutation } from '../hooks/useAssetHierarchy'
import { StatusChip } from './StatusChip'
import type { Asset } from '../types'

// Strictly two-level (FR-AST-04): a child never manages its own children, it
// only shows which asset it's a component of. Mirrors the backend's
// AssetHierarchyService rules so the UI never offers an action the API
// would reject.
export function AssetChildrenPanel({ asset }: { asset: Asset }) {
  const navigate = useNavigate()

  if (asset.parentAssetId) {
    return <ParentLink parentAssetId={asset.parentAssetId} />
  }

  return <ChildrenManager assetId={asset.id} onNavigate={(id) => navigate(`/assets/${id}`)} />
}

function ParentLink({ parentAssetId }: { parentAssetId: string }) {
  const navigate = useNavigate()
  const parentQuery = useAssetQuery(parentAssetId)

  return (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Component Of
      </Typography>
      {parentQuery.data ? (
        <Link component="button" variant="body2" onClick={() => navigate(`/assets/${parentAssetId}`)}>
          {parentQuery.data.assetNumber} · {parentQuery.data.name}
        </Link>
      ) : (
        <Typography variant="body2" color="text.secondary">
          Loading…
        </Typography>
      )}
    </Box>
  )
}

function ChildrenManager({ assetId, onNavigate }: { assetId: string; onNavigate: (id: string) => void }) {
  const childrenQuery = useAssetChildrenQuery(assetId)
  const linkChild = useLinkChildMutation(assetId)
  const unlinkChild = useUnlinkChildMutation(assetId)

  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 300)
  const candidatesQuery = useAssetsQuery({ q: debouncedSearch }, 0, 10)

  const children = childrenQuery.data ?? []
  const childIds = new Set(children.map((c) => c.id))
  const candidates = (candidatesQuery.data?.data ?? []).filter(
    (a) => a.id !== assetId && !childIds.has(a.id) && !a.parentAssetId,
  )

  return (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Components
      </Typography>

      {children.length === 0 ? (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          No components linked yet.
        </Typography>
      ) : (
        <Stack spacing={1} sx={{ mb: 2 }}>
          {children.map((child) => (
            <Stack key={child.id} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <Link component="button" variant="body2" onClick={() => onNavigate(child.id)} sx={{ flexGrow: 1, textAlign: 'left' }}>
                {child.assetNumber} · {child.name}
              </Link>
              <StatusChip status={child.status} />
              <IconButton
                size="small"
                aria-label={`Unlink ${child.assetNumber}`}
                disabled={unlinkChild.isPending}
                onClick={() => unlinkChild.mutate(child.id)}
              >
                <DeleteOutlineIcon fontSize="small" />
              </IconButton>
            </Stack>
          ))}
        </Stack>
      )}

      <Autocomplete
        size="small"
        options={candidates}
        loading={candidatesQuery.isFetching}
        getOptionLabel={(option) => `${option.assetNumber} · ${option.name}`}
        isOptionEqualToValue={(option, value) => option.id === value.id}
        inputValue={searchInput}
        onInputChange={(_, value) => setSearchInput(value)}
        onChange={(_, value) => {
          if (value) {
            linkChild.mutate(value.id)
            setSearchInput('')
          }
        }}
        value={null}
        renderInput={(params) => <TextField {...params} label="Add component" placeholder="Search by asset number or name" />}
      />
    </Box>
  )
}
