package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssetAssignmentRequest(
        @NotNull UUID personId,
        @NotNull Long version
) {
}
