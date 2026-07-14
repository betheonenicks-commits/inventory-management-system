import { useQuery } from '@tanstack/react-query'
import { fetchAssets } from '../../../api/assets/assetApi'

/** Scan-entry's asset picker: this codebase has no lookup-by-code endpoint yet (EPIC-SRC untouched), so this reuses the plain asset list/search. */
export function useAssetSearchQuery(q: string) {
  return useQuery({
    queryKey: ['AUD', 'assetSearch', q],
    queryFn: () => fetchAssets({ q }, 0, 10),
    enabled: q.trim().length > 1,
  })
}
