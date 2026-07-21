import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createOrgNode,
  deleteOrgNode,
  fetchOrgLevels,
  fetchOrgNodes,
  renameOrgLevel,
} from '../../../api/org/orgNodeApi'
import type { OrgNodeCreatePayload } from '../../../api/org/orgNodeApi'

export function useOrgNodesQuery() {
  return useQuery({ queryKey: ['ORG', 'nodes'], queryFn: fetchOrgNodes })
}

export function useOrgLevelsQuery() {
  return useQuery({ queryKey: ['ORG', 'levels'], queryFn: fetchOrgLevels })
}

export function useCreateOrgNodeMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: OrgNodeCreatePayload) => createOrgNode(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ORG', 'nodes'] }),
  })
}

export function useDeleteOrgNodeMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteOrgNode(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ORG', 'nodes'] }),
  })
}

export function useRenameOrgLevelMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name, version }: { id: string; name: string; version: number }) =>
      renameOrgLevel(id, name, version),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ORG', 'levels'] }),
  })
}
