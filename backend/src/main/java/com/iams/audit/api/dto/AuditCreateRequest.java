package com.iams.audit.api.dto;

import com.iams.audit.domain.AuditType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AuditCreateRequest(
        @NotBlank String name,
        @NotNull AuditType auditType,
        UUID scopeOrgNodeId,
        UUID scopeCategoryId,
        List<UUID> assetIds,
        @NotNull UUID nominalApproverId,
        LocalDate scheduledDate
) {
}
