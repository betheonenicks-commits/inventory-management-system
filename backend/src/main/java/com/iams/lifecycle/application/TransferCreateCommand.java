package com.iams.lifecycle.application;

import com.iams.lifecycle.domain.ChildDisposition;
import java.util.Map;
import java.util.UUID;

/**
 * US-LIF-05: create-transfer request. toPersonId is optional - a transfer can be location-only, or also change custodian.
 * US-AST-04: childDispositions maps each child asset id to how it's handled when the parent moves; every child of the
 * asset must be present, or create() blocks.
 */
public record TransferCreateCommand(
        UUID assetId,
        UUID toOrgNodeId,
        UUID toPersonId,
        String reason,
        UUID nominalApproverId,
        Map<UUID, ChildDisposition> childDispositions
) {
}
