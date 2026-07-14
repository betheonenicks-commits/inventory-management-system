// Mirrors backend/src/main/java/com/iams/usr/api/dto/RoleResponse.java

export interface Role {
  id: string
  version: number
  code: string
  name: string
  description: string | null
  system: boolean
  sensitive: boolean
  assignableToHumans: boolean
  permissions: string[]
}
