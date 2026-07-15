import { httpClient } from '../httpClient'
import type { LifecycleRequestStatus } from '../../features/lifecycle/types'
import type { PurchaseOrder } from '../../features/procurement/types'
import type {
  CostingMethod,
  InventoryItem,
  InventoryItemCostingMethodChange,
  InventoryStockBalance,
  InventoryTransaction,
  LowStockItem,
  ManualAdjustment,
  UnitOfMeasure,
  Vendor,
  Warehouse,
} from '../../features/inventory/types'

// --- Warehouses (US-INV-03) ---

export function fetchWarehouses() {
  return httpClient.get<Warehouse[]>('/warehouses').then((r) => r.data)
}

export function fetchWarehouse(id: string) {
  return httpClient.get<Warehouse>(`/warehouses/${id}`).then((r) => r.data)
}

export function createWarehouse(name: string, code: string, orgNodeId: string) {
  return httpClient.post<Warehouse>('/warehouses', { name, code, orgNodeId }).then((r) => r.data)
}

export function updateWarehouse(id: string, name: string) {
  return httpClient.patch<Warehouse>(`/warehouses/${id}`, { name }).then((r) => r.data)
}

export function deactivateWarehouse(id: string) {
  return httpClient.post<Warehouse>(`/warehouses/${id}/deactivate`).then((r) => r.data)
}

// --- Vendors (US-INV-07/08) ---

export function fetchVendors() {
  return httpClient.get<Vendor[]>('/vendors').then((r) => r.data)
}

export function createVendor(name: string, contactEmail?: string, contactPhone?: string) {
  return httpClient.post<Vendor>('/vendors', { name, contactEmail, contactPhone }).then((r) => r.data)
}

export function updateVendor(id: string, name: string, contactEmail?: string, contactPhone?: string) {
  return httpClient.patch<Vendor>(`/vendors/${id}`, { name, contactEmail, contactPhone }).then((r) => r.data)
}

export function deactivateVendor(id: string) {
  return httpClient.post<Vendor>(`/vendors/${id}/deactivate`).then((r) => r.data)
}

export function fetchVendorPurchaseOrders(id: string) {
  return httpClient.get<PurchaseOrder[]>(`/vendors/${id}/purchase-orders`).then((r) => r.data)
}

// --- Inventory items (US-INV-01/06/11) ---

export function fetchInventoryItems(activeOnly = false) {
  return httpClient.get<InventoryItem[]>('/inventory-items', { params: { activeOnly } }).then((r) => r.data)
}

export function fetchInventoryItem(id: string) {
  return httpClient.get<InventoryItem>(`/inventory-items/${id}`).then((r) => r.data)
}

export function createInventoryItem(
  name: string,
  sku: string,
  unitOfMeasure: UnitOfMeasure,
  reorderLevel?: number,
  costingMethod?: CostingMethod,
) {
  return httpClient
    .post<InventoryItem>('/inventory-items', { name, sku, unitOfMeasure, reorderLevel, costingMethod })
    .then((r) => r.data)
}

export function updateInventoryItem(id: string, name: string, reorderLevel?: number, costingMethod?: CostingMethod) {
  return httpClient.patch<InventoryItem>(`/inventory-items/${id}`, { name, reorderLevel, costingMethod }).then((r) => r.data)
}

export function deactivateInventoryItem(id: string) {
  return httpClient.post<InventoryItem>(`/inventory-items/${id}/deactivate`).then((r) => r.data)
}

export function fetchCostingMethodHistory(id: string) {
  return httpClient.get<InventoryItemCostingMethodChange[]>(`/inventory-items/${id}/costing-method-history`).then((r) => r.data)
}

// --- Stock movements (US-INV-02/04/06/08/09/10) ---

export interface StockInPayload {
  itemId: string
  warehouseId: string
  subLocation?: string
  lotNumber?: string
  expiryDate?: string
  quantity: number
  unitCost: number
  currencyCode?: string
  fxRate?: number
  reasonCode: string
}

export function stockIn(payload: StockInPayload) {
  return httpClient.post<InventoryStockBalance>('/inventory-stock/stock-in', payload).then((r) => r.data)
}

export interface StockOutPayload {
  itemId: string
  warehouseId: string
  subLocation?: string
  lotNumber?: string
  quantity: number
  reasonCode: string
}

export function stockOut(payload: StockOutPayload) {
  return httpClient.post<InventoryStockBalance>('/inventory-stock/stock-out', payload).then((r) => r.data)
}

export interface StockTransferPayload {
  itemId: string
  fromWarehouseId: string
  fromSubLocation?: string
  fromLotNumber?: string
  toWarehouseId: string
  toSubLocation?: string
  toLotNumber?: string
  quantity: number
  reasonCode: string
}

export function transferStock(payload: StockTransferPayload) {
  return httpClient.post<InventoryStockBalance[]>('/inventory-stock/transfer', payload).then((r) => r.data)
}

export function fetchBalances(itemId?: string, warehouseId?: string) {
  return httpClient.get<InventoryStockBalance[]>('/inventory-stock/balances', { params: { itemId, warehouseId } }).then((r) => r.data)
}

export function fetchTransactions(itemId: string) {
  return httpClient.get<InventoryTransaction[]>('/inventory-stock/transactions', { params: { itemId } }).then((r) => r.data)
}

export function fetchLowStock() {
  return httpClient.get<LowStockItem[]>('/inventory-stock/low-stock').then((r) => r.data)
}

export function fetchExpiringLots(withinDays = 30) {
  return httpClient.get<InventoryStockBalance[]>('/inventory-stock/expiring-lots', { params: { withinDays } }).then((r) => r.data)
}

// --- Manual adjustments (US-INV-05) ---

export function fetchManualAdjustments(status?: LifecycleRequestStatus) {
  return httpClient.get<ManualAdjustment[]>('/inventory-adjustments', { params: status ? { status } : undefined }).then((r) => r.data)
}

export function fetchManualAdjustment(id: string) {
  return httpClient.get<ManualAdjustment>(`/inventory-adjustments/${id}`).then((r) => r.data)
}

export function requestManualAdjustment(
  itemId: string,
  warehouseId: string,
  quantityDelta: number,
  reason: string,
  nominalApproverId: string,
  subLocation?: string,
  lotNumber?: string,
) {
  return httpClient
    .post<ManualAdjustment>('/inventory-adjustments', { itemId, warehouseId, quantityDelta, reason, nominalApproverId, subLocation, lotNumber })
    .then((r) => r.data)
}

export function approveManualAdjustment(id: string) {
  return httpClient.post<ManualAdjustment>(`/inventory-adjustments/${id}/approve`).then((r) => r.data)
}

export function rejectManualAdjustment(id: string, reason: string) {
  return httpClient.post<ManualAdjustment>(`/inventory-adjustments/${id}/reject`, { reason }).then((r) => r.data)
}
