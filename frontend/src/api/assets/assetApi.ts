import { httpClient } from '../httpClient'
import type { Asset, AssetHistoryEvent, AssetListFilters, PageResponse } from '../../features/assets/types'

export interface AssetCreatePayload {
  categoryId: string
  name: string
  manufacturer?: string
  model?: string
  serialNumber?: string
  vendorName?: string
  purchaseOrderReference?: string
  purchaseDate?: string
  purchaseCost?: number
  orgNodeId?: string
  warrantyStartDate?: string
  warrantyEndDate?: string
  rfidTagId?: string
  customFields?: Record<string, unknown>
}

export interface AssetUpdatePayload extends Partial<Omit<AssetCreatePayload, 'categoryId'>> {
  version: number
}

export function fetchAssets(filters: AssetListFilters, page: number, size: number) {
  return httpClient
    .get<PageResponse<Asset>>('/assets', { params: { ...filters, page, size } })
    .then((r) => r.data)
}

export function fetchAsset(id: string) {
  return httpClient.get<Asset>(`/assets/${id}`).then((r) => r.data)
}

export function createAsset(payload: AssetCreatePayload) {
  return httpClient.post<Asset>('/assets', payload).then((r) => r.data)
}

export function updateAsset(id: string, payload: AssetUpdatePayload) {
  return httpClient.patch<Asset>(`/assets/${id}`, payload).then((r) => r.data)
}

export function changeAssetStatus(id: string, statusId: string, version: number) {
  return httpClient.patch<Asset>(`/assets/${id}/status`, { statusId, version }).then((r) => r.data)
}

export function fetchAssetHistory(id: string, page: number, size: number) {
  return httpClient
    .get<PageResponse<AssetHistoryEvent>>(`/assets/${id}/history`, { params: { page, size } })
    .then((r) => r.data)
}

export function fetchAssetMovements(id: string, page: number, size: number) {
  return httpClient
    .get<PageResponse<AssetHistoryEvent>>(`/assets/${id}/movements`, { params: { page, size } })
    .then((r) => r.data)
}

export function labelUrl(id: string, format: 'png' | 'svg' | 'pdf', size = '50x25') {
  return `${httpClient.defaults.baseURL}/assets/${id}/label?format=${format}&size=${size}`
}

export function fetchAssetChildren(id: string) {
  return httpClient.get<Asset[]>(`/assets/${id}/children`).then((r) => r.data)
}

export function linkAssetChild(parentId: string, childAssetId: string) {
  return httpClient.post<Asset>(`/assets/${parentId}/children`, { childAssetId }).then((r) => r.data)
}

export function unlinkAssetChild(parentId: string, childId: string) {
  return httpClient.delete<void>(`/assets/${parentId}/children/${childId}`).then((r) => r.data)
}

export function assignAsset(assetId: string, personId: string, version: number) {
  return httpClient.post<Asset>(`/assets/${assetId}/assignment`, { personId, version }).then((r) => r.data)
}

export function unassignAsset(assetId: string, version: number) {
  return httpClient
    .delete<Asset>(`/assets/${assetId}/assignment`, { params: { version } })
    .then((r) => r.data)
}
