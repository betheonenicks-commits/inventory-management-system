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

// --- Saved searches (US-SRC-04) ---

export interface SavedSearch {
  id: string
  name: string
  query: string | null
  categoryId: string | null
  statusId: string | null
  orgNodeId: string | null
  purchasedFrom: string | null
  purchasedTo: string | null
}

export interface ResolvedSavedSearch extends SavedSearch {
  // AC-SRC-04: clauses referencing since-deleted entities are dropped, each
  // with a human-readable note - the rest still apply.
  droppedFilterNotes: string[]
}

export function fetchSavedSearches() {
  return httpClient.get<SavedSearch[]>('/saved-searches').then((r) => r.data)
}

export function createSavedSearch(payload: {
  name: string
  query?: string
  categoryId?: string
  statusId?: string
  orgNodeId?: string
  purchasedFrom?: string
  purchasedTo?: string
}) {
  return httpClient.post<SavedSearch>('/saved-searches', payload).then((r) => r.data)
}

export function deleteSavedSearch(id: string) {
  return httpClient.delete(`/saved-searches/${id}`).then(() => undefined)
}

export function resolveSavedSearch(id: string) {
  return httpClient.get<ResolvedSavedSearch>(`/saved-searches/${id}/resolved`).then((r) => r.data)
}

export function lookupAssetCode(code: string) {
  return httpClient.get<AssetHit>(`/search/asset-code/${encodeURIComponent(code)}`).then((r) => r.data)
}
