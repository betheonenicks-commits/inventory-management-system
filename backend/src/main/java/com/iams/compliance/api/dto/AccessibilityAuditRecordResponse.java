package com.iams.compliance.api.dto;

import com.iams.compliance.domain.AccessibilityAuditOutcome;
import java.time.LocalDate;
import java.util.UUID;

public record AccessibilityAuditRecordResponse(UUID id, long version, LocalDate auditDate, AccessibilityAuditOutcome outcome, String notes) {

    public static AccessibilityAuditRecordResponse notYetRecorded() {
        return new AccessibilityAuditRecordResponse(null, 0, null, null, "No accessibility audit has been recorded yet.");
    }
}
