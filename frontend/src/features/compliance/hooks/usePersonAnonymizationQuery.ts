import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { anonymizePerson, fetchAnonymizationEligiblePersons } from '../../../api/compliance/complianceApi'

export function useAnonymizationEligibleQuery() {
  return useQuery({
    queryKey: ['CMP', 'anonymizationEligible'],
    queryFn: fetchAnonymizationEligiblePersons,
  })
}

export function useAnonymizePersonMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (personId: string) => anonymizePerson(personId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'anonymizationEligible'] }),
  })
}
