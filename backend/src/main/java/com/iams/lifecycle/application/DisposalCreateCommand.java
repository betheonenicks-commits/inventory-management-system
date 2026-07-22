package com.iams.lifecycle.application;

import com.iams.lifecycle.domain.ChildDisposition;
import com.iams.lifecycle.domain.DisposalType;
import java.util.Map;
import java.util.UUID;

/**
 * US-LIF-09: create-disposal request.
 * US-AST-04: childDispositions maps each child asset id to how it's handled when the parent is disposed; every child
 * of the asset must be present, or create() blocks.
 */
public record DisposalCreateCommand(
        UUID assetId,
        DisposalType disposalType,
        String reason,
        UUID nominalApproverId,
        Map<UUID, ChildDisposition> childDispositions
) {
}
