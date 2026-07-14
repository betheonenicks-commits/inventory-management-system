import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchDepreciation,
  fetchDepreciationOverride,
  upsertDepreciationOverride,
} from '../../../api/assets/assetDepreciationApi'
import type { DepreciationOverridePayload } from '../../../api/assets/assetDepreciationApi'

export function useDepreciationQuery(assetId: string | undefined) {
  return useQuery({
    queryKey: ['AST', 'depreciation', assetId],
    queryFn: () => fetchDepreciation(assetId as string),
    enabled: !!assetId,
  })
}

export function useDepreciationOverrideQuery(assetId: string | undefined) {
  return useQuery({
    queryKey: ['AST', 'depreciationOverride', assetId],
    queryFn: () => fetchDepreciationOverride(assetId as string),
    enabled: !!assetId,
  })
}

export function useUpsertDepreciationOverrideMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: DepreciationOverridePayload) => upsertDepreciationOverride(assetId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'depreciation', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'depreciationOverride', assetId] })
    },
  })
}
