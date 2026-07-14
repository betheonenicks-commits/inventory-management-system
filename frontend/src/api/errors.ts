// Mirrors the backend's RFC 7807 problem+json shape (ApiExceptionHandler).
export interface ValidationErrorItem {
  field: string
  message: string
}

// Matches IAMS_API_Specification_v1.1.md Section 4.5's USER_HAS_OUTSTANDING_ASSIGNMENTS
// payload (UserDeactivationService), and generically anything else ConflictException's
// extraProperties carries.
export interface BlockingAsset {
  assetId: string
  assetNumber: string
  name: string
  assignedSince: string | null
}

export interface ApiProblem {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  errorCode: string
  traceId: string | null
  timestamp: string
  errors?: ValidationErrorItem[]
  expectedVersion?: number
  currentVersion?: number
  currentResource?: unknown
  blockingAssets?: BlockingAsset[]
  resolutionActions?: string[]
}

export function isApiProblem(value: unknown): value is ApiProblem {
  return (
    typeof value === 'object' &&
    value !== null &&
    'errorCode' in value &&
    'status' in value
  )
}
