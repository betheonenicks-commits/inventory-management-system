import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeAssetStatus, updateAsset } from '../../../api/assets/assetApi'
import type { AssetUpdatePayload } from '../../../api/assets/assetApi'

export function useUpdateAssetMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AssetUpdatePayload) => updateAsset(assetId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetMovements', assetId] })
    },
  })
}

export function useChangeAssetStatusMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { statusId: string; version: number }) =>
      changeAssetStatus(assetId, vars.statusId, vars.version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
    },
  })
}
