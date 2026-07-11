package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssetStatusChangeRequest(
        @NotNull UUID statusId,
        @NotNull Long version
) {
}
