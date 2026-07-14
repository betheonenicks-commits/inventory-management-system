package com.iams.usr.api.dto;

import java.util.UUID;

/**
 * Minimal, non-sensitive user projection for approver/assignee pickers (US-LIF-05/10,
 * US-PRC-01) - id and display name only. Deliberately not {@link UserResponse}: any
 * authenticated user needs to name a colleague as an approver, but only users:read
 * holders (Administrator+) should ever see username/email/roles/status.
 */
public record UserSummaryResponse(UUID id, String displayName) {
}
