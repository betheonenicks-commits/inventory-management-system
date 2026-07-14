import { httpClient } from '../httpClient'
import { isApiProblem } from '../errors'
import type { VehicleDetail } from '../../features/assets/types'

export interface VehicleDetailPayload {
  vin?: string
  registrationNumber?: string
  odometerReading?: number
  odometerUnit?: string
  registrationExpiryDate?: string
  insuranceExpiryDate?: string
  version?: number
}

// Returns null on 404 (no vehicle detail recorded yet) rather than throwing.
export function fetchVehicleDetail(assetId: string) {
  return httpClient
    .get<VehicleDetail>(`/assets/${assetId}/vehicle`)
    .then((r) => r.data)
    .catch((err) => {
      if (isApiProblem(err) && err.status === 404) return null
      throw err
    })
}

export function upsertVehicleDetail(assetId: string, payload: VehicleDetailPayload) {
  return httpClient.put<VehicleDetail>(`/assets/${assetId}/vehicle`, payload).then((r) => r.data)
}
