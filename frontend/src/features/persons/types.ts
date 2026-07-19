// Mirrors backend/src/main/java/com/iams/org/api/dto/PersonResponse.java

export type PersonType = 'EMPLOYEE' | 'VOLUNTEER'

export interface Person {
  id: string
  version: number
  fullName: string
  email: string | null
  personType: PersonType
  orgNodeId: string | null
  orgNodeName: string | null
  departmentId: string | null
  active: boolean
  createdBy: string
  createdAt: string
  updatedBy: string | null
  updatedAt: string | null
}
