package com.iams.usr.api;

import com.iams.usr.api.dto.RoleResponse;
import com.iams.usr.domain.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getVersion(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                role.isSensitive(),
                role.isAssignableToHumans(),
                role.getPermissions()
        );
    }
}
