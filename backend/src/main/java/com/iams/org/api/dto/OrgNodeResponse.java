package com.iams.org.api.dto;

import java.util.UUID;

public record OrgNodeResponse(
        UUID id,
        String name,
        String code,
        boolean active,
        UUID parentId,
        String parentName,
        UUID levelId,
        String levelName,
        String levelCode,
        String path,
        String roomVariant
) {
}
