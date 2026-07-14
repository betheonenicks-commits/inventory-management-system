package com.iams.audit.api.dto;

import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.AuditType;
import java.time.Instant;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        String name,
        AuditType auditType,
        UUID scopeOrgNodeId,
        String scopeOrgNodeName,
        UUID scopeCategoryId,
        String scopeCategoryName,
        AuditStatus status,
        UUID nominalApproverId,
        UUID effectiveApproverId,
        UUID submittedBy,
        Instant submittedAt,
        String signatureName,
        UUID approvedBy,
        Instant approvedAt,
        String lastRejectionReason,
        long version
) {
}
