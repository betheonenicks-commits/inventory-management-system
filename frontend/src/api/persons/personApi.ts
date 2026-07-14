import { httpClient } from '../httpClient'
import type { Person, PersonType } from '../../features/persons/types'

export interface PersonCreatePayload {
  fullName: string
  email?: string
  personType: PersonType
  orgNodeId?: string
}

export function fetchPersons(q?: string) {
  return httpClient.get<Person[]>('/persons', { params: q ? { q } : undefined }).then((r) => r.data)
}

export function fetchPerson(id: string) {
  return httpClient.get<Person>(`/persons/${id}`).then((r) => r.data)
}

export function createPerson(payload: PersonCreatePayload) {
  return httpClient.post<Person>('/persons', payload).then((r) => r.data)
}
