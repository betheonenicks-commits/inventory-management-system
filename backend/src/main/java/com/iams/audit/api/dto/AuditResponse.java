package com.iams.audit.api.dto;

import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.AuditType;
import java.time.Instant;
import java.time.LocalDate;
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
        LocalDate scheduledDate,
        // US-AUD-20: non-null only for a statistically-sampled audit (a sample of
        // samplingPopulationSize assets); all null on a normal 100% audit.
        Integer samplingConfidenceLevel,
        java.math.BigDecimal samplingMarginOfError,
        Integer samplingPopulationSize,
        long version
) {
}
