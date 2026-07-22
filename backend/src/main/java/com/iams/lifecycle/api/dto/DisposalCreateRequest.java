package com.iams.lifecycle.api.dto;

import com.iams.lifecycle.domain.ChildDisposition;
import com.iams.lifecycle.domain.DisposalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/** US-AST-04: childDispositions is required only when the asset has children; a childless asset may omit it. */
public record DisposalCreateRequest(
        @NotNull UUID assetId,
        @NotNull DisposalType disposalType,
        @NotBlank String reason,
        @NotNull UUID nominalApproverId,
        Map<UUID, ChildDisposition> childDispositions
) {
}
