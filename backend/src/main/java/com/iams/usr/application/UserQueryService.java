package com.iams.usr.application;

import com.iams.common.exception.NotFoundException;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of US-USR-01: "GET /auth/me shows role, scope, and computed
 * permissions" and the equivalent list/detail views for Administrators.
 * <p>
 * get()/list() fetch orgScopeNode eagerly (via the repository's JOIN FETCH
 * variants) because both are always followed by mapping to a response DTO
 * that reads orgScopeNode.getName() - a plain lazy findById() would throw
 * LazyInitializationException there, since open-in-view is disabled and the
 * mapping happens after this method (and its transaction) has returned.
 * getByUsername() (the login path) never touches org-scope-node data, so it
 * stays on the plain lookup.
 */
@Service
public class UserQueryService {

    private final AppUserRepository userRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;

    public UserQueryService(AppUserRepository userRepository, UserRoleAssignmentRepository roleAssignmentRepository) {
        this.userRepository = userRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<UserWithRoles> list() {
        List<AppUser> users = userRepository.findAllWithOrgScopeNodeOrderByDisplayNameAsc();
        List<UUID> userIds = users.stream().map(AppUser::getId).toList();

        // One query for all users' role assignments, not one per user (N+1).
        Map<UUID, List<Role>> rolesByUserId = roleAssignmentRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId(),
                        Collectors.mapping(UserRoleAssignment::getRole, Collectors.toList())));

        return users.stream()
                .map(user -> new UserWithRoles(user, rolesByUserId.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserWithRoles get(UUID id) {
        AppUser user = userRepository.findByIdWithOrgScopeNode(id).orElseThrow(() -> NotFoundException.of("User", id));
        return new UserWithRoles(user, rolesOf(id));
    }

    @Transactional(readOnly = true)
    public UserWithRoles getByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> NotFoundException.of("User", username));
        return new UserWithRoles(user, rolesOf(user.getId()));
    }

    private List<Role> rolesOf(UUID userId) {
        return roleAssignmentRepository.findByUserId(userId).stream()
                .map(UserRoleAssignment::getRole)
                .toList();
    }
}
