package com.iams.asset.infrastructure.label;

public enum LabelFormat {
    PNG("image/png"),
    SVG("image/svg+xml"),
    PDF("application/pdf");

    private final String contentType;

    LabelFormat(String contentType) {
        this.contentType = contentType;
    }

    public String contentType() {
        return contentType;
    }
}
