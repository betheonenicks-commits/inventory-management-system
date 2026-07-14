package com.iams.usr.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.PersonRepository;
import com.iams.sec.application.PasswordValidator;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import com.iams.usr.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-USR-01 (provision a user with role + org scope) and US-USR-07 (flat,
 * non-inheriting roles: a user needing multiple capabilities gets multiple
 * role rows, nothing implied).
 * <p>
 * Password strength is enforced by PasswordValidator against the
 * configurable policy (US-SEC-05) - no hardcoded floor here anymore.
 */
@Service
public class UserProvisioningService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;
    private final PasswordValidator passwordValidator;

    public UserProvisioningService(AppUserRepository userRepository, RoleRepository roleRepository,
                                    UserRoleAssignmentRepository roleAssignmentRepository,
                                    OrgNodeRepository orgNodeRepository, PersonRepository personRepository,
                                    PasswordEncoder passwordEncoder, CurrentUserProvider currentUserProvider,
                                    SecurityEventLogger securityEventLogger, PasswordValidator passwordValidator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.personRepository = personRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
        this.passwordValidator = passwordValidator;
    }

    @Transactional
    public AppUser create(String username, String password, String displayName, String email,
                           UUID personId, UUID orgScopeNodeId, Set<String> roleCodes) {
        if (username == null || username.isBlank()) {
            throw ValidationFailedException.singleField("username", "This field is required");
        }
        if (userRepository.existsByUsername(username)) {
            throw ValidationFailedException.singleField("username", "This username is already taken");
        }
        if (displayName == null || displayName.isBlank()) {
            throw ValidationFailedException.singleField("displayName", "This field is required");
        }
        passwordValidator.validate(password);
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw ValidationFailedException.singleField("roleCodes", "At least one role is required");
        }

        List<Role> roles = resolveRoles(roleCodes);
        CurrentUser actor = currentUserProvider.current();
        enforceRoleAssignmentRules(roles, actor);

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedBy(actor.id());

        if (personId != null) {
            if (!personRepository.existsById(personId)) {
                throw NotFoundException.of("Person", personId);
            }
            user.setPersonId(personId);
        }
        if (orgScopeNodeId != null) {
            user.setOrgScopeNode(orgNodeRepository.findById(orgScopeNodeId)
                    .orElseThrow(() -> NotFoundException.of("OrgNode", orgScopeNodeId)));
        }

        AppUser saved = userRepository.save(user);
        assignRoles(saved, roles, actor.id());
        return saved;
    }

    private List<Role> resolveRoles(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> ValidationFailedException.singleField("roleCodes", "Unknown role: " + code)))
                .toList();
    }

    /**
     * US-USR-01 exception AC: only a Super Administrator may assign a
     * security-sensitive role. FR-SEC-14: Integration Service is never
     * assignable to a human via this (human user provisioning) path.
     */
    private void enforceRoleAssignmentRules(List<Role> roles, CurrentUser actor) {
        boolean actorIsSuperAdmin = actor.roles().contains("SUPER_ADMIN");
        for (Role role : roles) {
            if (!role.isAssignableToHumans()) {
                throw ValidationFailedException.singleField("roleCodes",
                        role.getCode() + " cannot be assigned to a human user");
            }
            if (role.isSensitive() && !actorIsSuperAdmin) {
                throw ValidationFailedException.singleField("roleCodes",
                        role.getCode() + " can only be assigned by a Super Administrator");
            }
        }
    }

    private void assignRoles(AppUser user, List<Role> roles, UUID actorId) {
        Instant now = Instant.now();
        for (Role role : roles) {
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setId(UUID.randomUUID());
            assignment.setUser(user);
            assignment.setRole(role);
            assignment.setAssignedBy(actorId);
            assignment.setAssignedAt(now);
            roleAssignmentRepository.save(assignment);
            securityEventLogger.record(SecurityEventType.ROLE_ASSIGNED, actorId, user.getUsername(), null,
                    "Assigned role " + role.getCode());
        }
    }
}
