import { httpClient } from '../httpClient'

// EPIC-NTF: a user's own notifications, unread badge, and channel preferences.

export type NotificationEventType =
  | 'UPCOMING_AUDIT'
  | 'OVERDUE_AUDIT'
  | 'EXPIRY'
  | 'MAINTENANCE_DUE'
  | 'LOW_STOCK'
  | 'PENDING_APPROVAL'
  | 'SECURITY_ALERT'
  | 'ASSIGNMENT'
  | 'TRANSFER_DECISION'

export interface AppNotification {
  id: string
  eventType: NotificationEventType
  title: string
  body: string
  resourcePath: string | null
  createdAt: string
  readAt: string | null
}

export interface NotificationPage {
  content: AppNotification[]
  totalElements: number
}

export interface NotificationPreference {
  eventType: NotificationEventType
  emailEnabled: boolean
  locked: boolean
}

export function fetchNotifications(unreadOnly = false, page = 0, size = 10) {
  return httpClient
    .get<NotificationPage>('/notifications', { params: { unreadOnly, page, size } })
    .then((r) => r.data)
}

export function fetchUnreadCount() {
  return httpClient.get<{ count: number }>('/notifications/unread-count').then((r) => r.data.count)
}

export function markNotificationRead(id: string) {
  return httpClient.post<AppNotification>(`/notifications/${id}/read`).then((r) => r.data)
}

export function markAllNotificationsRead() {
  return httpClient.post<{ marked: number }>('/notifications/read-all').then((r) => r.data)
}

export function fetchNotificationPreferences() {
  return httpClient.get<NotificationPreference[]>('/notifications/preferences').then((r) => r.data)
}

export function updateNotificationPreference(eventType: NotificationEventType, emailEnabled: boolean) {
  return httpClient
    .put<NotificationPreference>(`/notifications/preferences/${eventType}`, { emailEnabled })
    .then((r) => r.data)
}
