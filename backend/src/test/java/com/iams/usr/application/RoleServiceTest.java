package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.common.security.StepUpGuard;
import com.iams.common.security.StepUpRequiredException;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleAssignmentRepository roleAssignmentRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private StepUpGuard stepUpGuard;

    private RoleService service;

    @BeforeEach
    void setUp() {
        service = new RoleService(roleRepository, roleAssignmentRepository, currentUserProvider, stepUpGuard);
    }

    private void stubActor() {
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "super", Set.of("SUPER_ADMIN")));
    }

    private Role customRole() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode("REGIONAL_COORDINATOR");
        role.setName("Regional Coordinator");
        role.setSystem(false);
        role.setVersion(0L);
        return role;
    }

    @Test
    void createCustom_succeeds() {
        stubActor();
        when(roleRepository.findByCode("REGIONAL_COORDINATOR")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role result = service.createCustom("REGIONAL_COORDINATOR", "Regional Coordinator", "desc",
                List.of("assets:read", "reports:read"));

        assertThat(result.getCode()).isEqualTo("REGIONAL_COORDINATOR");
        assertThat(result.isSystem()).isFalse();
        assertThat(result.isSensitive()).isFalse();
        assertThat(result.getPermissions()).containsExactly("assets:read", "reports:read");
    }

    @Test
    void createCustom_rejectsBlankCode() {
        stubActor();
        assertThatThrownBy(() -> service.createCustom("  ", "Name", null, List.of()))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void createCustom_rejectsDuplicateCode() {
        stubActor();
        when(roleRepository.findByCode("AUDITOR")).thenReturn(Optional.of(new Role()));

        assertThatThrownBy(() -> service.createCustom("AUDITOR", "Duplicate", null, List.of()))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void createCustom_requiresStepUp() {
        stubActor();
        Mockito.doThrow(new StepUpRequiredException()).when(stepUpGuard).requireVerified(any());

        assertThatThrownBy(() -> service.createCustom("REGIONAL_COORDINATOR", "Name", null, List.of()))
                .isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void updatePermissions_succeeds() {
        stubActor();
        Role role = customRole();
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(roleRepository.saveAndFlush(role)).thenReturn(role);

        Role result = service.updatePermissions(role.getId(), "New Name", null, List.of("audits:read"), 0L);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPermissions()).containsExactly("audits:read");
    }

    @Test
    void updatePermissions_rejectsSystemRole() {
        stubActor();
        Role systemRole = new Role();
        systemRole.setId(UUID.randomUUID());
        systemRole.setCode("SUPER_ADMIN");
        systemRole.setSystem(true);
        when(roleRepository.findById(systemRole.getId())).thenReturn(Optional.of(systemRole));

        assertThatThrownBy(() -> service.updatePermissions(systemRole.getId(), "New Name", null, null, 0L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatePermissions_rejectsStaleVersion() {
        stubActor();
        Role role = customRole();
        role.setVersion(3L);
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> service.updatePermissions(role.getId(), null, null, null, 2L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void delete_succeeds_whenNoAssignments() {
        stubActor();
        Role role = customRole();
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(roleAssignmentRepository.findByRoleId(role.getId())).thenReturn(List.of());

        service.delete(role.getId());
        // no exception - success
    }

    @Test
    void delete_blocked_whenRoleStillAssigned() {
        stubActor();
        Role role = customRole();
        AppUser assignedUser = new AppUser();
        assignedUser.setUsername("dnraike");
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setUser(assignedUser);
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(roleAssignmentRepository.findByRoleId(role.getId())).thenReturn(List.of(assignment));

        assertThatThrownBy(() -> service.delete(role.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("dnraike");
    }

    @Test
    void delete_blocked_whenSystemRole() {
        stubActor();
        Role systemRole = new Role();
        systemRole.setId(UUID.randomUUID());
        systemRole.setCode("AUDITOR");
        systemRole.setSystem(true);
        when(roleRepository.findById(systemRole.getId())).thenReturn(Optional.of(systemRole));

        assertThatThrownBy(() -> service.delete(systemRole.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatePermissions_requiresStepUp() {
        stubActor();
        Mockito.doThrow(new StepUpRequiredException()).when(stepUpGuard).requireVerified(any());

        assertThatThrownBy(() -> service.updatePermissions(UUID.randomUUID(), "New Name", null, null, 0L))
                .isInstanceOf(StepUpRequiredException.class);
    }

    @Test
    void delete_requiresStepUp() {
        stubActor();
        Mockito.doThrow(new StepUpRequiredException()).when(stepUpGuard).requireVerified(any());

        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(StepUpRequiredException.class);
    }
}
