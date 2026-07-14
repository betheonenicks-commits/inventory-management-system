import { httpClient } from '../httpClient'
import type { MaintenanceEvent, MaintenanceSchedule, MaintenanceType, RepairEvent } from '../../features/lifecycle/types'

// --- Repairs (US-LIF-06) ---

export function fetchRepairs(assetId?: string) {
  return httpClient.get<RepairEvent[]>('/repairs', { params: assetId ? { assetId } : undefined }).then((r) => r.data)
}

export function openRepair(
  assetId: string,
  vendorName: string | undefined,
  reason: string,
  estimatedCost: number | undefined,
  expectedReturnDate: string | undefined,
) {
  return httpClient.post<RepairEvent>('/repairs', { assetId, vendorName, reason, estimatedCost, expectedReturnDate }).then((r) => r.data)
}

export function closeRepair(id: string, actualReturnDate: string, actualCost: number | undefined) {
  return httpClient.post<RepairEvent>(`/repairs/${id}/close`, { actualReturnDate, actualCost }).then((r) => r.data)
}

// --- Preventive maintenance schedules (US-LIF-07) ---

export function fetchMaintenanceSchedules(assetId?: string) {
  return httpClient.get<MaintenanceSchedule[]>('/maintenance-schedules', { params: assetId ? { assetId } : undefined }).then((r) => r.data)
}

export function createMaintenanceSchedule(assetId: string, intervalMonths: number, nextDueDate: string, description: string | undefined) {
  return httpClient
    .post<MaintenanceSchedule>('/maintenance-schedules', { assetId, intervalMonths, nextDueDate, description })
    .then((r) => r.data)
}

export function deactivateMaintenanceSchedule(id: string) {
  return httpClient.post<MaintenanceSchedule>(`/maintenance-schedules/${id}/deactivate`).then((r) => r.data)
}

// --- Maintenance events (US-LIF-07/08) ---

export function fetchMaintenanceEvents(assetId?: string, maintenanceType?: MaintenanceType) {
  return httpClient.get<MaintenanceEvent[]>('/maintenance-events', { params: { assetId, maintenanceType } }).then((r) => r.data)
}

export function recordPreventiveMaintenance(scheduleId: string, notes: string | undefined, cost: number | undefined) {
  return httpClient.post<MaintenanceEvent>('/maintenance-events/preventive', { scheduleId, notes, cost }).then((r) => r.data)
}

export function recordCorrectiveMaintenance(assetId: string, notes: string, cost: number | undefined) {
  return httpClient.post<MaintenanceEvent>('/maintenance-events/corrective', { assetId, notes, cost }).then((r) => r.data)
}
