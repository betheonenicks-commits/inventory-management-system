import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createAsset } from '../../../api/assets/assetApi'

export function useCreateAssetMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createAsset,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
    },
  })
}
