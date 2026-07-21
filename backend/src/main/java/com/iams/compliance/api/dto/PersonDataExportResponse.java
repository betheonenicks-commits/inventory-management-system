package com.iams.compliance.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * US-SEC-10 (AC-SEC-10-H): "an export was available beforehand" - the full current
 * personal-data snapshot a Compliance Officer can pull before running erasure. Currently
 * assigned assets are included since they are the only other record directly linked to a
 * Person in this schema (historical/past assignments are not tracked by person id anywhere
 * queryable today, so are not claimed here).
 */
public record PersonDataExportResponse(
        UUID id,
        String fullName,
        String email,
        String personType,
        UUID orgNodeId,
        String orgNodeName,
        UUID departmentId,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<AssignedAsset> currentlyAssignedAssets,
        Instant exportedAt
) {
    public record AssignedAsset(UUID assetId, String assetNumber, String name) {
    }
}
