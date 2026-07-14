package com.iams.audit.api.dto;

import java.util.List;
import java.util.UUID;

public record AuditExceptionReportResponse(
        UUID auditId,
        boolean hasExceptions,
        List<AuditFindingResponse> findings
) {
}
