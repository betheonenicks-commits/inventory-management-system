import { httpClient } from '../httpClient'
import type { LifecycleRequestStatus } from '../../features/lifecycle/types'
import type {
  PurchaseOrder,
  PurchaseOrderLine,
  PurchaseOrderLineEvent,
  PurchaseOrderLineInput,
  PurchaseOrderStatus,
  PurchaseRequest,
} from '../../features/procurement/types'

// --- Purchase requests (US-LIF-01) ---

export function fetchPurchaseRequests(status?: LifecycleRequestStatus) {
  return httpClient.get<PurchaseRequest[]>('/purchase-requests', { params: status ? { status } : undefined }).then((r) => r.data)
}

export function fetchPurchaseRequest(id: string) {
  return httpClient.get<PurchaseRequest>(`/purchase-requests/${id}`).then((r) => r.data)
}

export function createPurchaseRequest(
  itemDescription: string,
  justification: string,
  estimatedCost: number | undefined,
  vendorName: string | undefined,
  nominalApproverId: string,
) {
  return httpClient
    .post<PurchaseRequest>('/purchase-requests', { itemDescription, justification, estimatedCost, vendorName, nominalApproverId })
    .then((r) => r.data)
}

export function approvePurchaseRequest(id: string) {
  return httpClient.post<PurchaseRequest>(`/purchase-requests/${id}/approve`).then((r) => r.data)
}

export function rejectPurchaseRequest(id: string, reason: string) {
  return httpClient.post<PurchaseRequest>(`/purchase-requests/${id}/reject`, { reason }).then((r) => r.data)
}

// --- Purchase orders (US-LIF-02/03/16) ---

export function fetchPurchaseOrders(status?: PurchaseOrderStatus) {
  return httpClient.get<PurchaseOrder[]>('/purchase-orders', { params: status ? { status } : undefined }).then((r) => r.data)
}

export function fetchPurchaseOrder(id: string) {
  return httpClient.get<PurchaseOrder>(`/purchase-orders/${id}`).then((r) => r.data)
}

export function createPurchaseOrder(purchaseRequestId: string, vendorName: string, lines: PurchaseOrderLineInput[]) {
  return httpClient.post<PurchaseOrder>('/purchase-orders', { purchaseRequestId, vendorName, lines }).then((r) => r.data)
}

export function fetchPurchaseOrderLines(id: string) {
  return httpClient.get<PurchaseOrderLine[]>(`/purchase-orders/${id}/lines`).then((r) => r.data)
}

export function cancelPurchaseOrder(id: string, reason: string) {
  return httpClient.post<PurchaseOrder>(`/purchase-orders/${id}/cancel`, { reason }).then((r) => r.data)
}

export function fetchLineEvents(lineId: string) {
  return httpClient.get<PurchaseOrderLineEvent[]>(`/purchase-orders/lines/${lineId}/events`).then((r) => r.data)
}

export function receiveLine(lineId: string, quantity: number, discrepancyNote?: string) {
  return httpClient.post<PurchaseOrderLine>(`/purchase-orders/lines/${lineId}/receive`, { quantity, discrepancyNote }).then((r) => r.data)
}

export function returnLineToVendor(lineId: string, quantity: number, reason: string) {
  return httpClient
    .post<PurchaseOrderLine>(`/purchase-orders/lines/${lineId}/return-to-vendor`, { quantity, reason })
    .then((r) => r.data)
}
