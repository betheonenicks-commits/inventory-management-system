import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createAssetCategory,
  deleteAssetCategory,
  fetchAssetCategories,
  updateAssetCategory,
} from '../../../api/assets/assetCategoryApi'
import type { AssetCategoryPayload } from '../../../api/assets/assetCategoryApi'

export function useAssetCategoriesQuery() {
  // Reference data changes rarely relative to assets - a longer staleTime is
  // appropriate here (per the plan's caching guidance), unlike asset list/detail.
  return useQuery({
    queryKey: ['AST', 'categories'],
    queryFn: fetchAssetCategories,
    staleTime: 5 * 60 * 1000,
  })
}

export function useCreateAssetCategoryMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createAssetCategory,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AST', 'categories'] }),
  })
}

export function useUpdateAssetCategoryMutation(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: AssetCategoryPayload) => updateAssetCategory(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AST', 'categories'] }),
  })
}

export function useDeleteAssetCategoryMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteAssetCategory(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['AST', 'categories'] }),
  })
}
