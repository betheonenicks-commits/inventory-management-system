package com.iams.analytics.application;

import java.util.UUID;

/**
 * US-ANL-04: raised when a feedback item is stored. Handled by
 * NotificationEventListeners (notification depends on analytics, never the
 * reverse - the same direction-of-dependency reasoning as the lifecycle
 * events), which routes it to the configured recipient role.
 */
public record FeedbackSubmittedEvent(UUID feedbackId, String category, String message, UUID submitterUserId,
                                     String submitterUsername, String pagePath) {
}
