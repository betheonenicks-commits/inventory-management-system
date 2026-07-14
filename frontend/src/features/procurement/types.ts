// Mirrors backend/src/main/java/com/iams/procurement/api/dto/*.java and com.iams.procurement.domain enums.

import type { LifecycleRequestStatus } from '../lifecycle/types'

export type PurchaseOrderStatus = 'OPEN' | 'CLOSED' | 'CANCELLED'
export type PurchaseOrderLineStatus = 'OPEN' | 'PARTIALLY_RECEIVED' | 'FULLY_RECEIVED' | 'CANCELLED'
export type PurchaseOrderLineEventType = 'RECEIVED' | 'CANCELLED' | 'RETURNED_TO_VENDOR'

export interface PurchaseRequest {
  id: string
  version: number
  itemDescription: string
  justification: string
  estimatedCost: number | null
  vendorName: string | null
  status: LifecycleRequestStatus
  nominalApproverId: string
  effectiveApproverId: string | null
  requestedBy: string
  requestedAt: string
  decidedBy: string | null
  decidedAt: string | null
  rejectionReason: string | null
}

export interface PurchaseOrder {
  id: string
  version: number
  poNumber: string
  purchaseRequestId: string
  vendorName: string
  status: PurchaseOrderStatus
}

export interface PurchaseOrderLine {
  id: string
  version: number
  purchaseOrderId: string
  description: string
  quantityOrdered: number
  quantityReceived: number
  quantityReturned: number
  unitCost: number
  status: PurchaseOrderLineStatus
}

export interface PurchaseOrderLineEvent {
  id: string
  lineId: string
  eventType: PurchaseOrderLineEventType
  quantity: number | null
  note: string | null
  actorId: string
  createdAt: string
}

export interface PurchaseOrderLineInput {
  description: string
  quantityOrdered: number
  unitCost: number
}
