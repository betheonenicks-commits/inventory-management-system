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

export function labelUrl(id: string, format: 'png' | 'svg' | 'pdf', size = '50x25') {
  return `${httpClient.defaults.baseURL}/assets/${id}/label?format=${format}&size=${size}`
}
