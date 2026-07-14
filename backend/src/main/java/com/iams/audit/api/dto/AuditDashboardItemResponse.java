package com.iams.audit.api.dto;

import com.iams.audit.domain.AuditStatus;
import java.util.UUID;

public record AuditDashboardItemResponse(
        UUID auditId,
        String name,
        AuditStatus status,
        AuditProgressResponse progress,
        long exceptionCount
) {
}
