import { httpClient } from '../httpClient'

// EPIC-SRC types, mirrored 1:1 against SearchService's hit records.

export interface AssetHit {
  id: string
  assetNumber: string
  name: string
  categoryName: string
  statusLabel: string
  orgNodeName: string
  serialNumber: string | null
  purchaseDate: string | null
}

export interface VendorHit {
  id: string
  name: string
  active: boolean
}

export interface PersonHit {
  id: string
  fullName: string
  orgNodeName: string | null
}

export interface GlobalSearchResult {
  assets: AssetHit[]
  vendors: VendorHit[]
  // false = the caller lacks inventory:read, so vendors were never searched -
  // distinct from "searched and nothing matched".
  vendorsSearched: boolean
  people: PersonHit[]
}

export function globalSearch(q: string) {
  return httpClient.get<GlobalSearchResult>('/search', { params: { q } }).then((r) => r.data)
}

export function lookupAssetCode(code: string) {
  return httpClient.get<AssetHit>(`/search/asset-code/${encodeURIComponent(code)}`).then((r) => r.data)
}
