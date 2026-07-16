import { useNavigate } from 'react-router-dom'
import Checkbox from '@mui/material/Checkbox'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import Paper from '@mui/material/Paper'
import { StatusChip } from './StatusChip'
import type { Asset, PageMeta } from '../types'

interface AssetTableProps {
  assets: Asset[]
  page: PageMeta
  onPageChange: (page: number) => void
  onPageSizeChange: (size: number) => void
  // US-RPT-11: selection for batch label printing. Optional - pages that
  // don't pass selection state render the table exactly as before.
  selectedIds?: Set<string>
  onToggleSelect?: (assetId: string) => void
  onToggleSelectPage?: (assetIds: string[]) => void
}

// Server-side pagination only - never a client "load-all", per NFR-SCALE-03
// at 100k+ assets. Page/size state is owned by the parent (AssetListPage),
// which encodes it into the URL so the list view is bookmarkable.
export function AssetTable({ assets, page, onPageChange, onPageSizeChange, selectedIds, onToggleSelect,
  onToggleSelectPage }: AssetTableProps) {
  const navigate = useNavigate()
  const selectable = !!selectedIds && !!onToggleSelect
  const pageIds = assets.map((a) => a.id)
  const selectedOnPage = selectable ? pageIds.filter((id) => selectedIds.has(id)).length : 0

  return (
    <Paper variant="outlined">
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              {selectable && (
                <TableCell padding="checkbox">
                  <Checkbox
                    size="small"
                    indeterminate={selectedOnPage > 0 && selectedOnPage < pageIds.length}
                    checked={pageIds.length > 0 && selectedOnPage === pageIds.length}
                    onChange={() => onToggleSelectPage?.(pageIds)}
                    slotProps={{ input: { 'aria-label': 'Select all assets on this page' } }}
                  />
                </TableCell>
              )}
              <TableCell>Asset Number</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Serial Number</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {assets.map((asset) => (
              <TableRow
                key={asset.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(`/assets/${asset.id}`)}
              >
                {selectable && (
                  <TableCell padding="checkbox" onClick={(e) => e.stopPropagation()}>
                    <Checkbox
                      size="small"
                      checked={selectedIds.has(asset.id)}
                      onChange={() => onToggleSelect(asset.id)}
                      slotProps={{ input: { 'aria-label': `Select ${asset.assetNumber}` } }}
                    />
                  </TableCell>
                )}
                <TableCell>{asset.assetNumber}</TableCell>
                <TableCell>{asset.name}</TableCell>
                <TableCell>{asset.categoryName}</TableCell>
                <TableCell>
                  <StatusChip status={asset.status} />
                </TableCell>
                <TableCell>{asset.serialNumber ?? '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        component="div"
        count={page.totalElements}
        page={page.number}
        rowsPerPage={page.size}
        rowsPerPageOptions={[25, 50, 100]}
        onPageChange={(_, newPage) => onPageChange(newPage)}
        onRowsPerPageChange={(e) => onPageSizeChange(Number(e.target.value))}
      />
    </Paper>
  )
}
