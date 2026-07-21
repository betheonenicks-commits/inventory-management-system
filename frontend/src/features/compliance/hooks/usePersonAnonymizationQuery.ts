import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { anonymizePerson, exportPersonData, fetchAnonymizationEligiblePersons } from '../../../api/compliance/complianceApi'

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

// US-SEC-10: "an export was available beforehand" - a mutation (not a query) since it's an
// on-demand pull triggered by an explicit button, not something rendered passively.
export function useExportPersonDataMutation() {
  return useMutation({ mutationFn: (personId: string) => exportPersonData(personId) })
}
