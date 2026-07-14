package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.PersonRepository;
import com.iams.sec.application.PasswordValidator;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleAssignmentRepository roleAssignmentRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private PersonRepository personRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;
    @Mock private PasswordValidator passwordValidator;

    private UserProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new UserProvisioningService(userRepository, roleRepository, roleAssignmentRepository,
                orgNodeRepository, personRepository, passwordEncoder, currentUserProvider, securityEventLogger,
                passwordValidator);
    }

    private void stubSuccessfulSave() {
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
    }

    private Role role(String code, boolean sensitive, boolean assignableToHumans) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setName(code);
        role.setSensitive(sensitive);
        role.setAssignableToHumans(assignableToHumans);
        return role;
    }

    private void stubActor(String... roles) {
        when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "actor", Set.of(roles)));
    }

    @Test
    void create_succeeds_withNonSensitiveRole() {
        stubSuccessfulSave();
        stubActor("ADMIN");
        when(userRepository.existsByUsername("dnraike")).thenReturn(false);
        when(roleRepository.findByCode("AUDITOR")).thenReturn(Optional.of(role("AUDITOR", false, true)));

        AppUser result = service.create("dnraike", "password123", "Devon Raike", null, null, null, Set.of("AUDITOR"));

        assertThat(result.getUsername()).isEqualTo("dnraike");
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void create_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("dnraike")).thenReturn(true);

        assertThatThrownBy(() -> service.create("dnraike", "password123", "Devon Raike", null, null, null, Set.of("AUDITOR")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsShortPassword() {
        org.mockito.Mockito.doThrow(ValidationFailedException.singleField("password", "Must be at least 8 characters"))
                .when(passwordValidator).validate("short");

        assertThatThrownBy(() -> service.create("dnraike", "short", "Devon Raike", null, null, null, Set.of("AUDITOR")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsEmptyRoles() {
        assertThatThrownBy(() -> service.create("dnraike", "password123", "Devon Raike", null, null, null, Set.of()))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownRoleCode() {
        when(userRepository.existsByUsername("dnraike")).thenReturn(false);
        when(roleRepository.findByCode("NOT_A_ROLE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("dnraike", "password123", "Devon Raike", null, null, null, Set.of("NOT_A_ROLE")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsSensitiveRole_whenActorIsNotSuperAdmin() {
        stubActor("ADMIN");
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(role("SUPER_ADMIN", true, true)));

        assertThatThrownBy(() -> service.create("newadmin", "password123", "New Admin", null, null, null, Set.of("SUPER_ADMIN")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_allowsSensitiveRole_whenActorIsSuperAdmin() {
        stubSuccessfulSave();
        stubActor("SUPER_ADMIN");
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(role("SUPER_ADMIN", true, true)));

        AppUser result = service.create("newadmin", "password123", "New Admin", null, null, null, Set.of("SUPER_ADMIN"));

        assertThat(result.getUsername()).isEqualTo("newadmin");
    }

    @Test
    void create_rejectsRoleNotAssignableToHumans() {
        stubActor("SUPER_ADMIN");
        when(userRepository.existsByUsername("svc")).thenReturn(false);
        when(roleRepository.findByCode("INTEGRATION_SERVICE"))
                .thenReturn(Optional.of(role("INTEGRATION_SERVICE", true, false)));

        assertThatThrownBy(() -> service.create("svc", "password123", "Service Account", null, null, null, Set.of("INTEGRATION_SERVICE")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownPerson() {
        stubActor("ADMIN");
        UUID personId = UUID.randomUUID();
        when(userRepository.existsByUsername("dnraike")).thenReturn(false);
        when(roleRepository.findByCode("AUDITOR")).thenReturn(Optional.of(role("AUDITOR", false, true)));
        when(personRepository.existsById(personId)).thenReturn(false);

        assertThatThrownBy(() -> service.create("dnraike", "password123", "Devon Raike", null, personId, null, Set.of("AUDITOR")))
                .isInstanceOf(NotFoundException.class);
    }
}
