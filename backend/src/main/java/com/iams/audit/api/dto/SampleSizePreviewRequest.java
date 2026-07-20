package com.iams.audit.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * US-AUD-20: request a sample-size preview for a prospective audit scope. Scope is
 * the same shape as audit creation (org node and/or category, or an explicit asset
 * list); confidenceLevel is required (90/95/99), marginOfError optional (defaults to 5%).
 */
public record SampleSizePreviewRequest(
        UUID scopeOrgNodeId,
        UUID scopeCategoryId,
        List<UUID> assetIds,
        @NotNull Integer confidenceLevel,
        Double marginOfError
) {
}
