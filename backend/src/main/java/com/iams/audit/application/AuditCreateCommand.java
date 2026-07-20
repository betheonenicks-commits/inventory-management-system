package com.iams.audit.application;

import com.iams.audit.domain.AuditType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * US-AUD-01/03/04: create-audit request. Scope is one of: scopeOrgNodeId
 * and/or scopeCategoryId (combined = intersection, e.g. "Building B, IT
 * Equipment"), or an explicit assetIds list - AuditService.create() resolves
 * whichever combination is given into the frozen expected-asset snapshot.
 */
public record AuditCreateCommand(
        String name,
        AuditType auditType,
        UUID scopeOrgNodeId,
        UUID scopeCategoryId,
        List<UUID> assetIds,
        UUID nominalApproverId,
        LocalDate scheduledDate,
        // US-AUD-20: optional statistical sampling. Both null (the common case) means a
        // full 100% audit - sampling is opt-in and never assumed. When set, the frozen
        // expected-asset set is a random sample of the resolved scope.
        Integer samplingConfidenceLevel,
        Double samplingMarginOfError
) {
    public boolean samplingRequested() {
        return samplingConfidenceLevel != null;
    }
}
