package com.iams.asset.api.dto;

import java.util.List;

public record LabelConfigResponse(
        List<SymbologyInfo> symbologies,
        List<LabelSizeResponse> sizes
) {
    public record SymbologyInfo(String type, String errorCorrectionLevel) {
    }

    public record LabelSizeResponse(String key, double widthMm, double heightMm) {
    }
}
