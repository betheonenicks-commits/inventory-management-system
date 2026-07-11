// Mirrors backend/src/main/java/com/iams/asset/api/dto/*.java exactly.

export type CustomFieldDataType = 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'ENUM'

export interface CustomFieldDefinition {
  id: string
  fieldKey: string
  label: string
  dataType: CustomFieldDataType
  required: boolean
  enumOptions: string[] | null
  displayOrder: number
}

export interface AssetCategory {
  id: string
  name: string
  code: string
  active: boolean
  version: number
  customFields: CustomFieldDefinition[]
}

export interface AssetStatus {
  id: string
  code: string
  label: string
  terminal: boolean
}

export interface Asset {
  id: string
  assetNumber: string
  version: number
  name: string
  categoryId: string
  categoryName: string
  status: AssetStatus
  orgNodeId: string
  orgNodeName: string
  assignedToPersonId: string | null
  parentAssetId: string | null
  serialNumber: string | null
  manufacturer: string | null
  model: string | null
  description: string | null
  barcode: { symbology: string; value: string }
  qrCode: { value: string; errorCorrectionLevel: string; labelUrl: string }
  vendorName: string | null
  purchaseOrderReference: string | null
  purchaseDate: string | null
  purchaseCost: number | null
  warrantyStartDate: string | null
  warrantyEndDate: string | null
  customFields: Record<string, unknown>
  createdBy: string
  createdAt: string
  updatedBy: string | null
  updatedAt: string | null
}

export interface AssetHistoryEvent {
  id: string
  eventType: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  correctionOfEventId: string | null
  createdBy: string
  createdAt: string
}

export interface PageMeta {
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PageResponse<T> {
  data: T[]
  page: PageMeta
  sort: string[]
}

export interface AssetListFilters {
  categoryId?: string
  statusId?: string
  q?: string
}
