import { httpClient } from '../httpClient'

// Mirrors backend/src/main/java/com/iams/org/api/dto/OrgNodeResponse.java
export interface OrgNode {
  id: string
  name: string
  code: string
  active: boolean
  parentId: string | null
  parentName: string | null
  levelId: string
  levelName: string
  levelCode: string
  path: string
  roomVariant: string | null
}

// Mirrors backend/src/main/java/com/iams/org/api/dto/OrgLevelResponse.java
export interface OrgLevel {
  id: string
  version: number
  code: string
  name: string
  rank: number
  roomVariants: string[]
}

export function fetchOrgNodes() {
  return httpClient.get<OrgNode[]>('/org-nodes').then((r) => r.data)
}

export function fetchOrgLevels() {
  return httpClient.get<OrgLevel[]>('/org-levels').then((r) => r.data)
}

// US-ORG-02: only the display label is renameable - code/rank are fixed at seed time.
export function renameOrgLevel(id: string, name: string, version: number) {
  return httpClient.patch<OrgLevel>(`/org-levels/${id}`, { name, version }).then((r) => r.data)
}

export interface OrgNodeCreatePayload {
  name: string
  code: string
  parentId?: string
  levelId: string
  roomVariant?: string
}

// US-ORG-01: build the hierarchy one node at a time (root when parentId is omitted).
export function createOrgNode(payload: OrgNodeCreatePayload) {
  return httpClient.post<OrgNode>('/org-nodes', payload).then((r) => r.data)
}

// US-ORG-01: blocked with 409 (dependent assets/people/child nodes) rather than a raw FK error.
export function deleteOrgNode(id: string) {
  return httpClient.delete<void>(`/org-nodes/${id}`).then((r) => r.data)
}
