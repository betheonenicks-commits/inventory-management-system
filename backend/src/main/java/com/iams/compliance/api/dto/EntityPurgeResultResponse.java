package com.iams.compliance.api.dto;

import com.iams.compliance.application.RetentionPolicyService.PurgeResult;
import com.iams.compliance.domain.RetentionEntityType;

/** US-CMP-01: the outcome of a per-entity-type retention purge run. */
public record EntityPurgeResultResponse(
        RetentionEntityType entityType,
        long purged,
        long skipped,
        String detail
) {
    public static EntityPurgeResultResponse from(PurgeResult result) {
        return new EntityPurgeResultResponse(result.entityType(), result.purged(), result.skipped(), result.detail());
    }
}
