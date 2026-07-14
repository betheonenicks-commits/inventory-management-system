package com.iams.lifecycle.application;

import java.util.UUID;

/** US-LIF-05: create-transfer request. toPersonId is optional - a transfer can be location-only, or also change custodian. */
public record TransferCreateCommand(
        UUID assetId,
        UUID toOrgNodeId,
        UUID toPersonId,
        String reason,
        UUID nominalApproverId
) {
}
