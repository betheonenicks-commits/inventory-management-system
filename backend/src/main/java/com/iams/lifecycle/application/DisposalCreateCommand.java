package com.iams.lifecycle.application;

import com.iams.lifecycle.domain.DisposalType;
import java.util.UUID;

/** US-LIF-09: create-disposal request. */
public record DisposalCreateCommand(
        UUID assetId,
        DisposalType disposalType,
        String reason,
        UUID nominalApproverId
) {
}
