package com.iams.asset.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssetChildLinkRequest(
        @NotNull UUID childAssetId
) {
}
