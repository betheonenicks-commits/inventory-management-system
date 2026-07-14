package com.iams.lifecycle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.domain.ApprovalDelegation;
import com.iams.lifecycle.domain.ApprovalDelegationRepository;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ApprovalRoutingServiceTest {

    @Mock private ApprovalDelegationRepository delegationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleAssignmentRepository roleAssignmentRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private ApprovalRoutingService service;
    private UUID delegatorId;

    @BeforeEach
    void setUp() {
        service = new ApprovalRoutingService(delegationRepository, appUserRepository, roleRepository, roleAssignmentRepository, currentUserProvider);
        delegatorId = UUID.randomUUID();
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(delegatorId, "depthead", Set.of("DEPARTMENT_HEAD")));
    }

    @Test
    void createDelegation_rejectsSelfDelegation() {
        assertThatThrownBy(() -> service.createDelegation(delegatorId, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), "leave"))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void createDelegation_rejectsUnknownDelegate() {
        UUID delegateId = UUID.randomUUID();
        when(appUserRepository.existsById(delegateId)).thenReturn(false);

        assertThatThrownBy(() -> service.createDelegation(delegateId, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), "leave"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createDelegation_rejectsValidToBeforeValidFrom() {
        UUID delegateId = UUID.randomUUID();
        when(appUserRepository.existsById(delegateId)).thenReturn(true);
        Instant from = Instant.now();

        assertThatThrownBy(() -> service.createDelegation(delegateId, from, from.minus(1, ChronoUnit.DAYS), "leave"))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void createDelegation_succeeds() {
        UUID delegateId = UUID.randomUUID();
        when(appUserRepository.existsById(delegateId)).thenReturn(true);
        when(delegationRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        Instant from = Instant.now();
        Instant to = from.plus(7, ChronoUnit.DAYS);

        ApprovalDelegation result = service.createDelegation(delegateId, from, to, "Annual leave");

        assertThat(result.getDelegatorUserId()).isEqualTo(delegatorId);
        assertThat(result.getDelegateUserId()).isEqualTo(delegateId);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void resolveEffectiveApprover_returnsDelegate_whenActiveDelegationExists() {
        UUID nominalApproverId = UUID.randomUUID();
        UUID delegateId = UUID.randomUUID();
        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setDelegateUserId(delegateId);
        when(delegationRepository.findActiveDelegation(org.mockito.ArgumentMatchers.eq(nominalApproverId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(delegation));

        UUID result = service.resolveEffectiveApprover(nominalApproverId);

        assertThat(result).isEqualTo(delegateId);
    }

    @Test
    void resolveEffectiveApprover_returnsNominal_whenNoActiveDelegation() {
        UUID nominalApproverId = UUID.randomUUID();
        when(delegationRepository.findActiveDelegation(org.mockito.ArgumentMatchers.eq(nominalApproverId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        UUID result = service.resolveEffectiveApprover(nominalApproverId);

        assertThat(result).isEqualTo(nominalApproverId);
    }

    @Test
    void resolveEscalationTarget_fallsBackToAdministrator_whenNoDelegate() {
        UUID currentApproverId = UUID.randomUUID();
        when(delegationRepository.findActiveDelegation(org.mockito.ArgumentMatchers.eq(currentApproverId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(adminRole));
        UUID adminUserId = UUID.randomUUID();
        AppUser adminUser = new AppUser();
        adminUser.setId(adminUserId);
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setUser(adminUser);
        when(roleAssignmentRepository.findByRoleId(adminRole.getId())).thenReturn(List.of(assignment));

        UUID result = service.resolveEscalationTarget(currentApproverId);

        assertThat(result).isEqualTo(adminUserId);
    }

    @Test
    void resolveEscalationTarget_throws_whenNoAdministratorExists() {
        UUID currentApproverId = UUID.randomUUID();
        when(delegationRepository.findActiveDelegation(org.mockito.ArgumentMatchers.eq(currentApproverId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleAssignmentRepository.findByRoleId(adminRole.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolveEscalationTarget(currentApproverId)).isInstanceOf(IllegalStateException.class);
    }
}
