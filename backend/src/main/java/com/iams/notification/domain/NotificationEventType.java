package com.iams.notification.domain;

/**
 * US-NTF-06's standard trigger catalog. Adding an event means adding a member
 * here, seed templates for it, and (if scheduled) a sweep rule in
 * NotificationTriggerJob - never custom dispatch code per event.
 */
public enum NotificationEventType {
    UPCOMING_AUDIT,
    OVERDUE_AUDIT,
    EXPIRY,
    MAINTENANCE_DUE,
    LOW_STOCK,
    PENDING_APPROVAL,
    SECURITY_ALERT,
    ASSIGNMENT,
    TRANSFER_DECISION;

    /**
     * US-NTF-05: Administrator-locked types - the email channel cannot be
     * turned off for these. Security alerts and approval-class events are
     * exactly what the stories name as must-reach traffic.
     */
    public boolean locked() {
        return this == SECURITY_ALERT || this == PENDING_APPROVAL;
    }

    /** US-NTF-08: exhausted retries on these escalate to an admin alert. */
    public boolean approvalClass() {
        return this == PENDING_APPROVAL || this == TRANSFER_DECISION;
    }
}
