import { httpClient } from '../httpClient'
import { isApiProblem } from '../errors'
import type { AssetInsurance } from '../../features/assets/types'

export interface AssetInsurancePayload {
  insurerName?: string
  policyNumber?: string
  coverageAmount?: number
  coverageCurrency?: string
  policyStartDate?: string
  policyExpiryDate?: string
  version?: number
}

// Returns null rather than throwing when no policy exists yet (404) - that's
// the normal, expected state for most assets, not an error condition.
export function fetchAssetInsurance(assetId: string) {
  return httpClient
    .get<AssetInsurance>(`/assets/${assetId}/insurance`)
    .then((r) => r.data)
    .catch((err) => {
      if (isApiProblem(err) && err.status === 404) return null
      throw err
    })
}

export function upsertAssetInsurance(assetId: string, payload: AssetInsurancePayload) {
  return httpClient.put<AssetInsurance>(`/assets/${assetId}/insurance`, payload).then((r) => r.data)
}
