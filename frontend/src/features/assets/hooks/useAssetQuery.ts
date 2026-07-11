import { useQuery } from '@tanstack/react-query'
import { fetchAsset, fetchAssetHistory } from '../../../api/assets/assetApi'

export function useAssetQuery(id: string | undefined) {
  return useQuery({
    queryKey: ['AST', 'asset', id],
    queryFn: () => fetchAsset(id as string),
    enabled: !!id,
  })
}

export function useAssetHistoryQuery(id: string | undefined, page = 0, size = 20) {
  return useQuery({
    queryKey: ['AST', 'assetHistory', id, page, size],
    queryFn: () => fetchAssetHistory(id as string, page, size),
    enabled: !!id,
  })
}
