package com.iams.audit.api.dto;

import com.iams.audit.domain.AssetCondition;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AuditScanRequest(
        @NotNull UUID assetId,
        @NotNull AssetCondition condition,
        String remarks,
        String deviceId
) {
}
