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

export function fetchOrgNodes() {
  return httpClient.get<OrgNode[]>('/org-nodes').then((r) => r.data)
}
