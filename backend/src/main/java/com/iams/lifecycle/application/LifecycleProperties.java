package com.iams.lifecycle.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * US-LIF-12/13's two configurable windows. Picked up automatically via
 * IamsApplication's @ConfigurationPropertiesScan, same as every other
 * *Properties class in this codebase - no explicit @EnableConfigurationProperties needed.
 */
@ConfigurationProperties(prefix = "iams.lifecycle")
public class LifecycleProperties {

    /** US-LIF-12: how many days after a disposal is approved it can still be restored. */
    private int restoreWindowDays = 30;

    /** US-LIF-13: how many hours a pending transfer/disposal request can sit untouched before it's eligible to escalate. */
    private int escalationThresholdHours = 72;

    public int getRestoreWindowDays() {
        return restoreWindowDays;
    }

    public void setRestoreWindowDays(int restoreWindowDays) {
        this.restoreWindowDays = restoreWindowDays;
    }

    public int getEscalationThresholdHours() {
        return escalationThresholdHours;
    }

    public void setEscalationThresholdHours(int escalationThresholdHours) {
        this.escalationThresholdHours = escalationThresholdHours;
    }
}
