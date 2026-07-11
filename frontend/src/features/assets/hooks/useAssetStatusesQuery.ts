import { useQuery } from '@tanstack/react-query'
import { fetchAssetStatuses } from '../../../api/assets/assetStatusApi'

export function useAssetStatusesQuery() {
  return useQuery({
    queryKey: ['AST', 'statuses'],
    queryFn: fetchAssetStatuses,
    staleTime: 5 * 60 * 1000,
  })
}
