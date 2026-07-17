import { httpClient } from '../httpClient'

// EPIC-ANL. Deliberately tiny: feedback submission is the analytics module's
// ONLY client-facing write (US-ANL-02) - usage events are captured
// server-side and have no submission API to call. The adoption report rides
// on the generic reports client (reportApi).

export type FeedbackCategory = 'BUG' | 'IDEA' | 'FRICTION' | 'PRAISE' | 'OTHER'

export interface FeedbackReceipt {
  id: string
  category: FeedbackCategory
  message: string | null
  pagePath: string | null
  createdAt: string
}

export function submitFeedback(category: FeedbackCategory, message: string, pagePath: string) {
  return httpClient
    .post<FeedbackReceipt>('/feedback', {
      category,
      message: message.trim() || undefined,
      pagePath: pagePath || undefined,
    })
    .then((r) => r.data)
}
