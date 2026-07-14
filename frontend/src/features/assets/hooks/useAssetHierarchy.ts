import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAssetChildren, linkAssetChild, unlinkAssetChild } from '../../../api/assets/assetApi'

export function useAssetChildrenQuery(assetId: string | undefined) {
  return useQuery({
    queryKey: ['AST', 'assetChildren', assetId],
    queryFn: () => fetchAssetChildren(assetId as string),
    enabled: !!assetId,
  })
}

export function useLinkChildMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (childAssetId: string) => linkAssetChild(assetId, childAssetId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetChildren', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
    },
  })
}

export function useUnlinkChildMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (childId: string) => unlinkAssetChild(assetId, childId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetChildren', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
    },
  })
}
