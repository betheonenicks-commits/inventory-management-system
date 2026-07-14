package com.iams.org.api.dto;

import java.util.List;
import java.util.UUID;

public record OrgLevelResponse(
        UUID id,
        long version,
        String code,
        String name,
        int rank,
        List<String> roomVariants
) {
}
