import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAssetInsurance, upsertAssetInsurance } from '../../../api/assets/assetInsuranceApi'
import type { AssetInsurancePayload } from '../../../api/assets/assetInsuranceApi'

export function useAssetInsuranceQuery(assetId: string | undefined) {
  return useQuery({
    queryKey: ['AST', 'assetInsurance', assetId],
    queryFn: () => fetchAssetInsurance(assetId as string),
    enabled: !!assetId,
  })
}

export function useUpsertAssetInsuranceMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AssetInsurancePayload) => upsertAssetInsurance(assetId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetInsurance', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
    },
  })
}
