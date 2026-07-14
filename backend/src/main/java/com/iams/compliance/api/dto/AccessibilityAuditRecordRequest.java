package com.iams.compliance.api.dto;

import com.iams.compliance.domain.AccessibilityAuditOutcome;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AccessibilityAuditRecordRequest(@NotNull LocalDate auditDate, @NotNull AccessibilityAuditOutcome outcome, String notes) {
}
