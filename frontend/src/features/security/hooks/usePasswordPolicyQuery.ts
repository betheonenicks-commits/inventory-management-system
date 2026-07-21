import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchPasswordPolicy, updatePasswordPolicy } from '../../../api/security/passwordPolicyApi'
import type { PasswordPolicyUpdatePayload } from '../../../api/security/passwordPolicyApi'

export function usePasswordPolicyQuery() {
  return useQuery({ queryKey: ['SEC', 'passwordPolicy'], queryFn: fetchPasswordPolicy, staleTime: 5 * 60 * 1000 })
}

export function useUpdatePasswordPolicyMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: PasswordPolicyUpdatePayload) => updatePasswordPolicy(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['SEC', 'passwordPolicy'] }),
  })
}
