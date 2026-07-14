package com.iams.audit.application;

import com.iams.audit.domain.AssetCondition;
import java.util.UUID;

/** US-AUD-05/10/12: a single scan submitted against an audit. */
public record AuditScanCommand(
        UUID assetId,
        AssetCondition condition,
        String remarks,
        String deviceId
) {
}
