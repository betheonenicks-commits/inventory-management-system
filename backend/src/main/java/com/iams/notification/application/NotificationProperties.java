package com.iams.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** EPIC-NTF wiring. Picked up via @ConfigurationPropertiesScan like every other *Properties class. */
@ConfigurationProperties(prefix = "iams.notifications")
public class NotificationProperties {

    /** US-NTF-01: master switch for the email channel; off degrades gracefully to in-app only. */
    private boolean mailEnabled = true;

    private String mailFrom = "iams@localhost";

    /** US-NTF-08: retry cap ("the default 3 attempts"). */
    private int maxAttempts = 3;

    /** US-NTF-08: first backoff step in seconds; doubles per attempt. */
    private long backoffBaseSeconds = 60;

    /** US-NTF-06 catalog knobs. */
    private int upcomingAuditDaysFirst = 7;
    private int upcomingAuditDaysSecond = 1;
    private int overdueRepeatDays = 3;
    private int expiryLookaheadDays = 30;

    /** US-ANL-04: the role whose holders receive submitted feedback; falls back to admins if nobody holds it. */
    private String feedbackRecipientRole = "ADMIN";

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public void setMailEnabled(boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBackoffBaseSeconds() {
        return backoffBaseSeconds;
    }

    public void setBackoffBaseSeconds(long backoffBaseSeconds) {
        this.backoffBaseSeconds = backoffBaseSeconds;
    }

    public int getUpcomingAuditDaysFirst() {
        return upcomingAuditDaysFirst;
    }

    public void setUpcomingAuditDaysFirst(int upcomingAuditDaysFirst) {
        this.upcomingAuditDaysFirst = upcomingAuditDaysFirst;
    }

    public int getUpcomingAuditDaysSecond() {
        return upcomingAuditDaysSecond;
    }

    public void setUpcomingAuditDaysSecond(int upcomingAuditDaysSecond) {
        this.upcomingAuditDaysSecond = upcomingAuditDaysSecond;
    }

    public int getOverdueRepeatDays() {
        return overdueRepeatDays;
    }

    public void setOverdueRepeatDays(int overdueRepeatDays) {
        this.overdueRepeatDays = overdueRepeatDays;
    }

    public int getExpiryLookaheadDays() {
        return expiryLookaheadDays;
    }

    public void setExpiryLookaheadDays(int expiryLookaheadDays) {
        this.expiryLookaheadDays = expiryLookaheadDays;
    }

    public String getFeedbackRecipientRole() {
        return feedbackRecipientRole;
    }

    public void setFeedbackRecipientRole(String feedbackRecipientRole) {
        this.feedbackRecipientRole = feedbackRecipientRole;
    }
}
