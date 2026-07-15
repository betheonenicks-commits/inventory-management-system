package com.iams.audit.api.dto;

import java.util.UUID;

/**
 * Minimal, non-sensitive audit projection for scope pickers outside EPIC-AUD
 * itself (e.g. US-CMP-06's audit-scoped legal hold) - id and name only.
 * Mirrors UserSummaryResponse's own "any authenticated user needs to name one
 * of these in an unrelated workflow, but only audits:read holders should see
 * the rest" reasoning.
 */
public record AuditSummaryResponse(UUID id, String name) {
}
