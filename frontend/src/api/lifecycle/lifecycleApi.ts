import { httpClient } from '../httpClient'
import type {
  ChildDisposition,
  Disposal,
  DisposalType,
  LifecycleRequestStatus,
  Transfer,
} from '../../features/lifecycle/types'

// --- Transfers (US-LIF-05/10/11/13) ---

export function fetchTransfers(assetId?: string, status?: LifecycleRequestStatus) {
  return httpClient.get<Transfer[]>('/transfers', { params: { assetId, status } }).then((r) => r.data)
}

// US-AST-04: childDispositions maps each component asset id to how it's handled; required only
// when the asset has children (the backend blocks a request that omits any).
export function createTransfer(
  assetId: string,
  toOrgNodeId: string,
  toPersonId: string | undefined,
  reason: string,
  nominalApproverId: string,
  childDispositions?: Record<string, ChildDisposition>,
) {
  return httpClient
    .post<Transfer>('/transfers', { assetId, toOrgNodeId, toPersonId, reason, nominalApproverId, childDispositions })
    .then((r) => r.data)
}

export function approveTransfer(id: string) {
  return httpClient.post<Transfer>(`/transfers/${id}/approve`).then((r) => r.data)
}

export function rejectTransfer(id: string, reason: string) {
  return httpClient.post<Transfer>(`/transfers/${id}/reject`, { reason }).then((r) => r.data)
}

export function escalateTransfer(id: string) {
  return httpClient.post<Transfer>(`/transfers/${id}/escalate`).then((r) => r.data)
}

// --- Disposals (US-LIF-09/10/11/12/13) ---

export function fetchDisposals(assetId?: string, status?: LifecycleRequestStatus) {
  return httpClient.get<Disposal[]>('/disposals', { params: { assetId, status } }).then((r) => r.data)
}

export function createDisposal(
  assetId: string,
  disposalType: DisposalType,
  reason: string,
  nominalApproverId: string,
  childDispositions?: Record<string, ChildDisposition>,
) {
  return httpClient
    .post<Disposal>('/disposals', { assetId, disposalType, reason, nominalApproverId, childDispositions })
    .then((r) => r.data)
}

export function approveDisposal(id: string) {
  return httpClient.post<Disposal>(`/disposals/${id}/approve`).then((r) => r.data)
}

export function rejectDisposal(id: string, reason: string) {
  return httpClient.post<Disposal>(`/disposals/${id}/reject`, { reason }).then((r) => r.data)
}

export function restoreDisposal(id: string) {
  return httpClient.post<Disposal>(`/disposals/${id}/restore`).then((r) => r.data)
}

export function escalateDisposal(id: string) {
  return httpClient.post<Disposal>(`/disposals/${id}/escalate`).then((r) => r.data)
}

// --- Approval delegation (US-LIF-15) ---

export interface ApprovalDelegation {
  id: string
  version: number
  delegatorUserId: string
  delegateUserId: string
  validFrom: string
  validTo: string
  active: boolean
  reason: string | null
}

export function fetchApprovalDelegations(delegatorUserId: string) {
  return httpClient
    .get<ApprovalDelegation[]>('/approval-delegations', { params: { delegatorUserId } })
    .then((r) => r.data)
}

export function createApprovalDelegation(delegateUserId: string, validFrom: string, validTo: string, reason?: string) {
  return httpClient
    .post<ApprovalDelegation>('/approval-delegations', { delegateUserId, validFrom, validTo, reason })
    .then((r) => r.data)
}

export function revokeApprovalDelegation(id: string) {
  return httpClient.post<ApprovalDelegation>(`/approval-delegations/${id}/revoke`).then((r) => r.data)
}
