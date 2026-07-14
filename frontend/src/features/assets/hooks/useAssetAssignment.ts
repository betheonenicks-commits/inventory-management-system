import { useMutation, useQueryClient } from '@tanstack/react-query'
import { assignAsset, unassignAsset } from '../../../api/assets/assetApi'

export function useAssignAssetMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { personId: string; version: number }) => assignAsset(assetId, vars.personId, vars.version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
    },
  })
}

export function useUnassignAssetMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (version: number) => unassignAsset(assetId, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
    },
  })
}
