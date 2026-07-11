import { httpClient } from '../httpClient'
import type { AssetStatus } from '../../features/assets/types'

export function fetchAssetStatuses() {
  return httpClient.get<AssetStatus[]>('/asset-statuses').then((r) => r.data)
}
