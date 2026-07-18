import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { assignAsset, assignAssetToDepartment, unassignAsset } from '../../../api/assets/assetApi'
import { fetchDepartments } from '../../../api/org/departmentApi'

const invalidateAssignment = (queryClient: ReturnType<typeof useQueryClient>, assetId: string) => {
  queryClient.invalidateQueries({ queryKey: ['AST', 'asset', assetId] })
  queryClient.invalidateQueries({ queryKey: ['AST', 'assets'] })
  queryClient.invalidateQueries({ queryKey: ['AST', 'assetHistory', assetId] })
}

export function useAssignAssetMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { personId: string; version: number }) => assignAsset(assetId, vars.personId, vars.version),
    onSuccess: () => invalidateAssignment(queryClient, assetId),
  })
}

export function useAssignAssetToDepartmentMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { departmentId: string; version: number }) =>
      assignAssetToDepartment(assetId, vars.departmentId, vars.version),
    onSuccess: () => invalidateAssignment(queryClient, assetId),
  })
}

// Only fetched while the picker is open, per the enabled-gating discipline.
export function useDepartmentsQuery(enabled: boolean) {
  return useQuery({ queryKey: ['ORG', 'departments'], queryFn: fetchDepartments, enabled })
}

export function useUnassignAssetMutation(assetId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (version: number) => unassignAsset(assetId, version),
    onSuccess: () => invalidateAssignment(queryClient, assetId),
  })
}
