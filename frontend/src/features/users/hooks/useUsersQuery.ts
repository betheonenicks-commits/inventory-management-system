import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createUser, deactivateUser, fetchPickableUsers, fetchUser, fetchUsers } from '../../../api/users/userApi'
import type { UserCreatePayload } from '../../../api/users/userApi'

export function useUsersQuery(enabled = true) {
  return useQuery({
    queryKey: ['USR', 'users'],
    queryFn: fetchUsers,
    enabled,
  })
}

/** For approver/assignee pickers - any authenticated user, not just users:read holders. */
export function usePickableUsersQuery(enabled = true) {
  return useQuery({
    queryKey: ['USR', 'pickable'],
    queryFn: fetchPickableUsers,
    enabled,
  })
}

export function useUserQuery(id: string | null | undefined) {
  return useQuery({
    queryKey: ['USR', 'user', id],
    queryFn: () => fetchUser(id as string),
    enabled: !!id,
  })
}

export function useCreateUserMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: UserCreatePayload) => createUser(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['USR', 'users'] }),
  })
}

export function useDeactivateUserMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => deactivateUser(id, version),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['USR', 'users'] }),
  })
}
