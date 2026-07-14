package com.iams.usr.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-USR-02: custom roles with a configurable permission set. System roles
 * (the nine FR-USR-01 defaults, the two system-provided custom roles, and
 * Integration Service) are never editable or deletable through this service -
 * only roles created here (is_system = false) are. Controller-level
 * @PreAuthorize restricts creation/editing to Super Administrators; this
 * service additionally guards the is_system invariant so it holds even if a
 * future caller forgets the annotation.
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public RoleService(RoleRepository roleRepository, UserRoleAssignmentRepository roleAssignmentRepository,
                        CurrentUserProvider currentUserProvider) {
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<Role> list() {
        return roleRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Role get(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> NotFoundException.of("Role", id));
    }

    @Transactional
    public Role createCustom(String code, String name, String description, List<String> permissions) {
        if (code == null || code.isBlank()) {
            throw ValidationFailedException.singleField("code", "This field is required");
        }
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "This field is required");
        }
        if (roleRepository.findByCode(code).isPresent()) {
            throw ValidationFailedException.singleField("code", "A role with this code already exists");
        }

        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setSystem(false);
        role.setSensitive(false);
        role.setAssignableToHumans(true);
        role.setPermissions(permissions != null ? new ArrayList<>(permissions) : new ArrayList<>());
        role.setCreatedBy(currentUserProvider.current().id());

        return roleRepository.save(role);
    }

    @Transactional
    public Role updatePermissions(UUID id, String name, String description, List<String> permissions, long expectedVersion) {
        Role role = get(id);
        rejectIfSystem(role, "modified");
        if (role.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, role.getVersion(), role);
        }

        if (name != null) {
            if (name.isBlank()) {
                throw ValidationFailedException.singleField("name", "This field is required");
            }
            role.setName(name);
        }
        if (description != null) {
            role.setDescription(description);
        }
        if (permissions != null) {
            role.setPermissions(new ArrayList<>(permissions));
        }
        role.setUpdatedBy(currentUserProvider.current().id());

        try {
            return roleRepository.saveAndFlush(role);
        } catch (OptimisticLockingFailureException e) {
            Role current = get(id);
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Role role = get(id);
        rejectIfSystem(role, "deleted");

        List<UserRoleAssignment> assignments = roleAssignmentRepository.findByRoleId(id);
        if (!assignments.isEmpty()) {
            List<String> affectedUsernames = assignments.stream()
                    .map(UserRoleAssignment::getUser)
                    .map(AppUser::getUsername)
                    .toList();
            throw new ConflictException("ROLE_HAS_ASSIGNED_USERS",
                    "Role '" + role.getName() + "' is still assigned to " + affectedUsernames.size()
                            + " user(s) and cannot be deleted: " + affectedUsernames);
        }

        roleRepository.delete(role);
    }

    /**
     * A system role's immutability is a business-rule/state conflict (409), not a request
     * validation failure (400) - the request body is perfectly well-formed, and resubmitting
     * it unchanged will never succeed, so ValidationFailedException's "fix your input and
     * retry" contract doesn't apply here. It also had no "code" field to point to:
     * RoleUpdateRequest carries name/description/permissions/version, never code.
     */
    private void rejectIfSystem(Role role, String attemptedAction) {
        if (role.isSystem()) {
            throw new ConflictException("SYSTEM_ROLE_IMMUTABLE",
                    "System role '" + role.getCode() + "' cannot be " + attemptedAction);
        }
    }
}
