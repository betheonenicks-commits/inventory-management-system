// Mirrors backend/src/main/java/com/iams/inventory/api/dto/*.java and com.iams.inventory.domain enums.

import type { LifecycleRequestStatus } from '../lifecycle/types'

export type UnitOfMeasure = 'EACH' | 'BOX' | 'KG' | 'LITRE' | 'PACK' | 'ROLL'
export type CostingMethod = 'WEIGHTED_AVERAGE' | 'LAST_COST'
export type InventoryTransactionType = 'STOCK_IN' | 'STOCK_OUT' | 'ADJUSTMENT' | 'TRANSFER_OUT' | 'TRANSFER_IN'

export interface Warehouse {
  id: string
  version: number
  name: string
  code: string
  orgNodeId: string
  orgNodeName: string
  active: boolean
}

export interface Vendor {
  id: string
  version: number
  name: string
  contactEmail: string | null
  contactPhone: string | null
  active: boolean
}

export interface InventoryItem {
  id: string
  version: number
  name: string
  sku: string
  unitOfMeasure: UnitOfMeasure
  reorderLevel: number | null
  costingMethod: CostingMethod
  active: boolean
}

export interface InventoryItemCostingMethodChange {
  id: string
  itemId: string
  oldMethod: CostingMethod
  newMethod: CostingMethod
  changedBy: string
  changedAt: string
}

export interface InventoryStockBalance {
  id: string
  itemId: string
  itemName: string
  sku: string
  unitOfMeasure: UnitOfMeasure
  warehouseId: string
  warehouseName: string
  subLocation: string
  lotNumber: string
  expiryDate: string | null
  quantityOnHand: number
  averageUnitCost: number | null
  version: number
}

export interface InventoryTransaction {
  id: string
  itemId: string
  itemName: string
  warehouseId: string
  warehouseName: string
  subLocation: string
  lotNumber: string
  expiryDate: string | null
  transactionType: InventoryTransactionType
  quantity: number
  unitCost: number | null
  currencyCode: string | null
  fxRate: number | null
  reportingUnitCost: number | null
  reasonCode: string
  performedByUserId: string
  performedByUsername: string
  performedAt: string
  linkedTransactionId: string | null
}

export interface LowStockItem {
  itemId: string
  name: string
  sku: string
  unitOfMeasure: UnitOfMeasure
  reorderLevel: number
  totalQuantity: number
}

export interface ManualAdjustment {
  id: string
  version: number
  itemId: string
  itemName: string
  warehouseId: string
  warehouseName: string
  subLocation: string
  lotNumber: string
  quantityDelta: number
  reason: string
  status: LifecycleRequestStatus
  nominalApproverId: string
  effectiveApproverId: string | null
  requestedBy: string
  requestedAt: string
  decidedBy: string | null
  decidedAt: string | null
  rejectionReason: string | null
  resultingTransactionId: string | null
}
