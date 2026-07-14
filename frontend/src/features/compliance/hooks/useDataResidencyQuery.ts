import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import { deleteOutboundFlow, fetchDataResidency, fetchOutboundFlows, saveOutboundFlow } from '../../../api/compliance/complianceApi'

export function useDataResidencyQuery() {
  return useQuery({
    queryKey: ['CMP', 'dataResidency'],
    queryFn: fetchDataResidency,
  })
}

export function useOutboundFlowsQuery() {
  return useQuery({
    queryKey: ['CMP', 'outboundFlows'],
    queryFn: fetchOutboundFlows,
  })
}

function invalidateResidencyData(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: ['CMP', 'dataResidency'] })
  queryClient.invalidateQueries({ queryKey: ['CMP', 'outboundFlows'] })
}

export function useSaveOutboundFlowMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ name, enabled, complianceReviewNote }: { name: string; enabled: boolean; complianceReviewNote?: string }) =>
      saveOutboundFlow(name, enabled, complianceReviewNote),
    onSuccess: () => invalidateResidencyData(queryClient),
  })
}

export function useDeleteOutboundFlowMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteOutboundFlow(id),
    onSuccess: () => invalidateResidencyData(queryClient),
  })
}
