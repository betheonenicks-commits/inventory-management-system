package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.SodWaiver;
import com.iams.usr.domain.SodWaiverRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SodWaiverServiceTest {

    @Mock private SodWaiverRepository sodWaiverRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRoleAssignmentRepository roleAssignmentRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private SodWaiverService service;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        service = new SodWaiverService(sodWaiverRepository, appUserRepository, roleAssignmentRepository, currentUserProvider);
        requesterId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(requesterId, "superadmin", Set.of("SUPER_ADMIN")));
    }

    private AppUser userWithRole(UUID id, String username, String roleCode) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        when(appUserRepository.findById(id)).thenReturn(Optional.of(user));

        Role role = new Role();
        role.setCode(roleCode);
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setUser(user);
        assignment.setRole(role);
        when(roleAssignmentRepository.findByUserId(id)).thenReturn(List.of(assignment));
        return user;
    }

    @Test
    void create_succeeds_whenSignerIsItSecurityOfficer() {
        UUID signerId = UUID.randomUUID();
        userWithRole(signerId, "secofficer", "IT_SECURITY_OFFICER");
        SodWaiver saved = new SodWaiver();
        saved.setId(UUID.randomUUID());
        when(sodWaiverRepository.save(org.mockito.ArgumentMatchers.any(SodWaiver.class))).thenReturn(saved);
        when(sodWaiverRepository.findByIdWithSignedOffBy(saved.getId())).thenReturn(Optional.of(saved));

        SodWaiver result = service.create("AUDIT_APPROVAL", signerId, "Single-admin site, no second administrator available");

        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_rejectsSelfAssertedSignOff() {
        assertThatThrownBy(() -> service.create("AUDIT_APPROVAL", requesterId, "Trying to sign off my own waiver"))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("self-asserted");
    }

    @Test
    void create_rejectsSignerWithoutItSecurityOfficerRole() {
        UUID signerId = UUID.randomUUID();
        userWithRole(signerId, "inventorymgr", "INVENTORY_MANAGER");

        assertThatThrownBy(() -> service.create("AUDIT_APPROVAL", signerId, "reason"))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("IT Security Officer");
    }
}
