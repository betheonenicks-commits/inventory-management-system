import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createWarehouse,
  deactivateWarehouse,
  fetchWarehouses,
  updateWarehouse,
} from '../../../api/inventory/inventoryApi'

export function useWarehousesQuery(enabled = true) {
  return useQuery({ queryKey: ['INV', 'warehouses'], queryFn: fetchWarehouses, enabled })
}

export function useCreateWarehouseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ name, code, orgNodeId }: { name: string; code: string; orgNodeId: string }) =>
      createWarehouse(name, code, orgNodeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'warehouses'] }),
  })
}

export function useUpdateWarehouseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => updateWarehouse(id, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'warehouses'] }),
  })
}

export function useDeactivateWarehouseMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deactivateWarehouse(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'warehouses'] }),
  })
}
