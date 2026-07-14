import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchVehicleDetail, upsertVehicleDetail } from '../../../api/assets/assetVehicleApi'
import type { VehicleDetailPayload } from '../../../api/assets/assetVehicleApi'

export function useVehicleDetailQuery(assetId: string | undefined) {
  return useQuery({
    queryKey: ['AST', 'vehicleDetail', assetId],
    queryFn: () => fetchVehicleDetail(assetId as string),
    enabled: !!assetId,
  })
}

export function useUpsertVehicleDetailMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: VehicleDetailPayload) => upsertVehicleDetail(assetId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['AST', 'vehicleDetail', assetId] })
      queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
    },
  })
}
