import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createVendor,
  deactivateVendor,
  fetchVendorPurchaseOrders,
  fetchVendors,
  updateVendor,
} from '../../../api/inventory/inventoryApi'

export function useVendorsQuery() {
  return useQuery({ queryKey: ['INV', 'vendors'], queryFn: fetchVendors })
}

export function useVendorPurchaseOrdersQuery(vendorId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['INV', 'vendorPurchaseOrders', vendorId],
    queryFn: () => fetchVendorPurchaseOrders(vendorId as string),
    enabled: !!vendorId && enabled,
  })
}

export function useCreateVendorMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ name, contactEmail, contactPhone }: { name: string; contactEmail?: string; contactPhone?: string }) =>
      createVendor(name, contactEmail, contactPhone),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'vendors'] }),
  })
}

export function useUpdateVendorMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name, contactEmail, contactPhone }: { id: string; name: string; contactEmail?: string; contactPhone?: string }) =>
      updateVendor(id, name, contactEmail, contactPhone),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'vendors'] }),
  })
}

export function useDeactivateVendorMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deactivateVendor(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['INV', 'vendors'] }),
  })
}
