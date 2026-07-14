import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createPerson, fetchPerson, fetchPersons } from '../../../api/persons/personApi'
import type { PersonCreatePayload } from '../../../api/persons/personApi'

export function usePersonsQuery(q: string | undefined) {
  return useQuery({
    queryKey: ['ORG', 'persons', q],
    queryFn: () => fetchPersons(q),
  })
}

export function usePersonQuery(id: string | null | undefined) {
  return useQuery({
    queryKey: ['ORG', 'person', id],
    queryFn: () => fetchPerson(id as string),
    enabled: !!id,
  })
}

export function useCreatePersonMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: PersonCreatePayload) => createPerson(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ORG', 'persons'] })
    },
  })
}
