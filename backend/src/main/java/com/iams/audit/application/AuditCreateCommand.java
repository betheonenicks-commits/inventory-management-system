package com.iams.audit.application;

import com.iams.audit.domain.AuditType;
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
        UUID nominalApproverId
) {
}
