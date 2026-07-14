import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createRole, deleteRole, fetchRole, fetchRoles, updateRole } from '../../../api/roles/roleApi'
import type { RoleCreatePayload, RoleUpdatePayload } from '../../../api/roles/roleApi'

export function useRolesQuery() {
  // Role catalog changes rarely relative to users - same reasoning as asset categories.
  return useQuery({
    queryKey: ['USR', 'roles'],
    queryFn: fetchRoles,
    staleTime: 5 * 60 * 1000,
  })
}

export function useRoleQuery(id: string | null | undefined) {
  return useQuery({
    queryKey: ['USR', 'role', id],
    queryFn: () => fetchRole(id as string),
    enabled: !!id,
  })
}

export function useCreateRoleMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: RoleCreatePayload) => createRole(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['USR', 'roles'] }),
  })
}

export function useUpdateRoleMutation(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: RoleUpdatePayload) => updateRole(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['USR', 'roles'] }),
  })
}

export function useDeleteRoleMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteRole(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['USR', 'roles'] }),
  })
}
