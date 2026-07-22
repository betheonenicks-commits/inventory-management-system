import { useQuery } from '@tanstack/react-query'
import { fetchSystemHealth } from '../../../api/system/systemApi'

export function useSystemHealthQuery() {
  return useQuery({
    queryKey: ['SYS', 'health'],
    queryFn: fetchSystemHealth,
    refetchInterval: 30 * 1000,
  })
}
