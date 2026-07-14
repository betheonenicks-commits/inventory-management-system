package com.iams.lifecycle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** US-LIF-05: toPersonId is optional - a transfer can be location-only, or also change custodian. */
public record TransferCreateRequest(
        @NotNull UUID assetId,
        @NotNull UUID toOrgNodeId,
        UUID toPersonId,
        @NotBlank String reason,
        @NotNull UUID nominalApproverId
) {
}
