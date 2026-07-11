package com.iams.asset.api.dto;

import java.util.UUID;

public record AssetStatusResponse(UUID id, String code, String label, boolean terminal, int sortOrder) {
}
