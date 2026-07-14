package com.iams.lifecycle.api.dto;

import com.iams.lifecycle.domain.DisposalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DisposalCreateRequest(
        @NotNull UUID assetId,
        @NotNull DisposalType disposalType,
        @NotBlank String reason,
        @NotNull UUID nominalApproverId
) {
}
