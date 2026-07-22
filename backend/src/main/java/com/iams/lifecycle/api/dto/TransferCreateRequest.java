package com.iams.lifecycle.api.dto;

import com.iams.lifecycle.domain.ChildDisposition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * US-LIF-05: toPersonId is optional - a transfer can be location-only, or also change custodian.
 * US-AST-04: childDispositions is required only when the asset has children (the service blocks a
 * request that omits any child); a childless asset may send an empty map or null.
 */
public record TransferCreateRequest(
        @NotNull UUID assetId,
        @NotNull UUID toOrgNodeId,
        UUID toPersonId,
        @NotBlank String reason,
        @NotNull UUID nominalApproverId,
        Map<UUID, ChildDisposition> childDispositions
) {
}
