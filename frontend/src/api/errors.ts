// Mirrors the backend's RFC 7807 problem+json shape (ApiExceptionHandler).
export interface ValidationErrorItem {
  field: string
  message: string
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
}

export function isApiProblem(value: unknown): value is ApiProblem {
  return (
    typeof value === 'object' &&
    value !== null &&
    'errorCode' in value &&
    'status' in value
  )
}
