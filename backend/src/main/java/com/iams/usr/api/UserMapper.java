package com.iams.usr.api;

import com.iams.usr.api.dto.UserResponse;
import com.iams.usr.api.dto.UserSummaryResponse;
import com.iams.usr.application.UserWithRoles;
import com.iams.usr.domain.Role;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(UserWithRoles userWithRoles) {
        var user = userWithRoles.user();
        return new UserSummaryResponse(user.getId(), user.getDisplayName());
    }

    public UserResponse toResponse(UserWithRoles userWithRoles) {
        var user = userWithRoles.user();
        return new UserResponse(
                user.getId(),
                user.getVersion(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPersonId(),
                user.getOrgScopeNode() != null ? user.getOrgScopeNode().getId() : null,
                user.getOrgScopeNode() != null ? user.getOrgScopeNode().getName() : null,
                user.getStatus(),
                userWithRoles.roles().stream().map(Role::getCode).collect(Collectors.toSet()),
                user.getCreatedBy(),
                user.getCreatedAt(),
                user.getUpdatedBy(),
                user.getUpdatedAt()
        );
    }
}
