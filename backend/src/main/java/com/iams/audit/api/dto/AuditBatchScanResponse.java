package com.iams.audit.api.dto;

import java.util.List;
import java.util.UUID;

public record AuditBatchScanResponse(
        List<AuditFindingResponse> created,
        List<UUID> duplicateAssetIds,
        List<UUID> unrecognizedAssetIds,
        Summary summary
) {
    public record Summary(int verifiedCount, int outOfScopeCount, int duplicateCount, int unrecognizedCount) {
    }
}
