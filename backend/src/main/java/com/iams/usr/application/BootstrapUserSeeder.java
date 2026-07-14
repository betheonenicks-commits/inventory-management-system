package com.iams.usr.application;

import com.iams.common.security.DevSecurityProperties;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import com.iams.usr.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first Super Administrator at startup, if no user with that
 * username exists yet. Replaces the AuthController hardcoded-DevUser stub
 * with a real app_user row - but reuses the exact same config keys
 * (iams.security.dev-user.*) so an operator's existing environment
 * variables/compose overrides keep working unchanged.
 * <p>
 * The password is hashed here, at startup, with the injected PasswordEncoder
 * bean - the same encoder AuthController now uses to verify it at login -
 * rather than a hash computed offline and pasted into a SQL migration.
 * <p>
 * The bootstrap user's id is fixed to the same UUID every seed migration
 * before this one already used as created_by for its reference rows
 * (asset_status_def, role_definition, etc.), so those historical references
 * now resolve to a real row instead of a UUID that belongs to no one.
 */
@Component
@Order(0)
public class BootstrapUserSeeder implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final DevSecurityProperties properties;

    public BootstrapUserSeeder(AppUserRepository userRepository, RoleRepository roleRepository,
                                UserRoleAssignmentRepository roleAssignmentRepository,
                                PasswordEncoder passwordEncoder, DevSecurityProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DevSecurityProperties.DevUser bootstrap = properties.getDevUser();
        if (userRepository.existsByUsername(bootstrap.getUsername())) {
            return;
        }

        Role role = roleRepository.findByCode(bootstrap.getRole())
                .orElseThrow(() -> new IllegalStateException(
                        "Bootstrap role '" + bootstrap.getRole() + "' is not seeded - check V15__create_role_definition.sql"));

        UUID bootstrapId = bootstrap.getId();

        AppUser user = new AppUser();
        user.setId(bootstrapId);
        user.setUsername(bootstrap.getUsername());
        user.setPasswordHash(passwordEncoder.encode(bootstrap.getPassword()));
        user.setDisplayName(bootstrap.getDisplayName());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedBy(bootstrapId); // the first Super Administrator is created by no one but itself
        userRepository.save(user);

        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setUser(user);
        assignment.setRole(role);
        assignment.setAssignedBy(bootstrapId);
        assignment.setAssignedAt(Instant.now());
        roleAssignmentRepository.save(assignment);
    }
}
