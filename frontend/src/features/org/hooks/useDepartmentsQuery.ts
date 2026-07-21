import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  updateDepartment,
} from '../../../api/org/departmentApi'
import type { DepartmentCreatePayload, DepartmentUpdatePayload } from '../../../api/org/departmentApi'

export function useDepartmentsAdminQuery() {
  return useQuery({ queryKey: ['ORG', 'departments'], queryFn: fetchDepartments })
}

export function useCreateDepartmentMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: DepartmentCreatePayload) => createDepartment(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ORG', 'departments'] }),
  })
}

export function useUpdateDepartmentMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: DepartmentUpdatePayload }) => updateDepartment(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ORG', 'departments'] }),
  })
}

export function useDeleteDepartmentMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteDepartment(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ORG', 'departments'] }),
  })
}
