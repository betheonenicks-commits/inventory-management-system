package com.iams.sec.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.sec.domain.PasswordPolicy;
import com.iams.sec.domain.PasswordPolicyRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock private PasswordPolicyRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;

    private PasswordPolicyService service;

    @BeforeEach
    void setUp() {
        service = new PasswordPolicyService(repository, currentUserProvider);
    }

    private PasswordPolicy policy() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setId(UUID.randomUUID());
        policy.setMinLength(8);
        policy.setVersion(0L);
        return policy;
    }

    @Test
    void get_returnsTheSeededRow() {
        PasswordPolicy policy = policy();
        when(repository.findAll()).thenReturn(List.of(policy));

        assertThat(service.get()).isSameAs(policy);
    }

    @Test
    void get_throwsIllegalState_whenNoRowSeeded() {
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_appliesChanges() {
        PasswordPolicy policy = policy();
        when(repository.findAll()).thenReturn(List.of(policy));
        when(repository.saveAndFlush(policy)).thenReturn(policy);
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "super", Set.of("SUPER_ADMIN")));

        PasswordPolicy result = service.update(12, true, true, true, true, 0L);

        assertThat(result.getMinLength()).isEqualTo(12);
        assertThat(result.isRequireUppercase()).isTrue();
        assertThat(result.isRequireDigit()).isTrue();
    }

    @Test
    void update_rejectsStaleVersion() {
        PasswordPolicy policy = policy();
        policy.setVersion(3L);
        when(repository.findAll()).thenReturn(List.of(policy));

        assertThatThrownBy(() -> service.update(12, null, null, null, null, 2L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }
}
