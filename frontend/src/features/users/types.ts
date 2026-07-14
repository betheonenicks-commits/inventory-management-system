// Mirrors backend/src/main/java/com/iams/usr/api/dto/UserResponse.java

export type UserStatus = 'ACTIVE' | 'DEACTIVATED'

export interface User {
  id: string
  version: number
  username: string
  displayName: string
  email: string | null
  personId: string | null
  orgScopeNodeId: string | null
  orgScopeNodeName: string | null
  status: UserStatus
  roleCodes: string[]
  createdBy: string
  createdAt: string
  updatedBy: string | null
  updatedAt: string | null
}

// Mirrors backend/src/main/java/com/iams/usr/api/dto/UserSummaryResponse.java
export interface UserSummary {
  id: string
  displayName: string
}
