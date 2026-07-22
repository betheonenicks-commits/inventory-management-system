package com.iams.platform.api.dto;

import java.time.Instant;
import java.util.Map;

/** US-USR-05 (AC-USR-05-H): the System Operator's technical-configuration view of app health. */
public record SystemHealthResponse(
        String status,
        Map<String, String> components,
        Instant checkedAt
) {
}
