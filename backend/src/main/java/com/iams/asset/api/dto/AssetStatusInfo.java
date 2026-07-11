package com.iams.asset.api.dto;

import java.util.UUID;

public record AssetStatusInfo(UUID id, String code, String label, boolean terminal) {
}
