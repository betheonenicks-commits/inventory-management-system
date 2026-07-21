import { useQuery } from '@tanstack/react-query'
import { searchSecurityEvents } from '../../../api/security/securityEventApi'
import type { SecurityEventSearchParams } from '../../../api/security/securityEventApi'

export function useSecurityEventsQuery(params: SecurityEventSearchParams) {
  return useQuery({
    queryKey: ['SEC', 'events', params],
    queryFn: () => searchSecurityEvents(params),
  })
}
