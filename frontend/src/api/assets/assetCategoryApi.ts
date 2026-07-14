import { httpClient } from '../httpClient'
import type { AssetCategory, CustomFieldDataType } from '../../features/assets/types'

export interface CustomFieldDefinitionPayload {
  fieldKey: string
  label: string
  dataType: CustomFieldDataType
  required: boolean
  enumOptions?: string[]
}

export interface AssetCategoryPayload {
  name: string
  code: string
  active?: boolean
  version?: number
  customFields?: CustomFieldDefinitionPayload[]
  requiresVehicleFields?: boolean
  defaultDepreciationMethod?: 'STRAIGHT_LINE' | 'DECLINING_BALANCE'
  defaultUsefulLifeMonths?: number
  defaultSalvageValuePct?: number
}

export function fetchAssetCategories() {
  return httpClient.get<AssetCategory[]>('/asset-categories').then((r) => r.data)
}

export function fetchAssetCategory(id: string) {
  return httpClient.get<AssetCategory>(`/asset-categories/${id}`).then((r) => r.data)
}

export function createAssetCategory(payload: AssetCategoryPayload) {
  return httpClient.post<AssetCategory>('/asset-categories', payload).then((r) => r.data)
}

export function updateAssetCategory(id: string, payload: AssetCategoryPayload) {
  return httpClient.patch<AssetCategory>(`/asset-categories/${id}`, payload).then((r) => r.data)
}

export function deleteAssetCategory(id: string) {
  return httpClient.delete(`/asset-categories/${id}`)
}
