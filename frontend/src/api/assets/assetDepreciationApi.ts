import { httpClient } from '../httpClient'
import { isApiProblem } from '../errors'
import type { DepreciationMethod, DepreciationOverride, DepreciationResult } from '../../features/assets/types'

export interface DepreciationOverridePayload {
  method?: DepreciationMethod
  usefulLifeMonths?: number
  salvageValuePct?: number
  depreciationStartDate?: string
  version?: number
}

export function fetchDepreciation(assetId: string, asOf?: string) {
  return httpClient
    .get<DepreciationResult>(`/assets/${assetId}/depreciation`, { params: asOf ? { asOf } : undefined })
    .then((r) => r.data)
}

// Returns null on 404 (no override recorded yet) rather than throwing.
export function fetchDepreciationOverride(assetId: string) {
  return httpClient
    .get<DepreciationOverride>(`/assets/${assetId}/depreciation-override`)
    .then((r) => r.data)
    .catch((err) => {
      if (isApiProblem(err) && err.status === 404) return null
      throw err
    })
}

export function upsertDepreciationOverride(assetId: string, payload: DepreciationOverridePayload) {
  return httpClient.put<DepreciationOverride>(`/assets/${assetId}/depreciation-override`, payload).then((r) => r.data)
}
