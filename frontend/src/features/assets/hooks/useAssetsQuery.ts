import { useQuery } from '@tanstack/react-query'
import { fetchAssets } from '../../../api/assets/assetApi'
import type { AssetListFilters } from '../types'

export function useAssetsQuery(filters: AssetListFilters, page: number, size: number) {
  return useQuery({
    queryKey: ['AST', 'assets', filters, page, size],
    queryFn: () => fetchAssets(filters, page, size),
  })
}
