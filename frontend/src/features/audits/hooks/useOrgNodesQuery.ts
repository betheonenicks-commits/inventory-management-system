import { useQuery } from '@tanstack/react-query'
import { fetchOrgNodes } from '../../../api/org/orgNodeApi'

export function useOrgNodesQuery() {
  return useQuery({
    queryKey: ['ORG', 'orgNodes'],
    queryFn: fetchOrgNodes,
    staleTime: 5 * 60 * 1000,
  })
}
