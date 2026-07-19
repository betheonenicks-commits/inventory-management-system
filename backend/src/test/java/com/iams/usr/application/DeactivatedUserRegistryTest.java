package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.UserStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeactivatedUserRegistryTest {

    @Mock private AppUserRepository userRepository;

    @Test
    void seedsFromAlreadyDeactivatedUsersAtStartup_soRevocationSurvivesRestart() {
        UUID gone = UUID.randomUUID();
        UUID active = UUID.randomUUID();
        when(userRepository.findIdsByStatus(UserStatus.DEACTIVATED)).thenReturn(List.of(gone));

        DeactivatedUserRegistry registry = new DeactivatedUserRegistry(userRepository);
        registry.run(null);

        assertThat(registry.isRevoked(gone)).isTrue();
        assertThat(registry.isRevoked(active)).isFalse();
    }

    @Test
    void markDeactivated_revokesImmediately() {
        when(userRepository.findIdsByStatus(UserStatus.DEACTIVATED)).thenReturn(List.of());
        DeactivatedUserRegistry registry = new DeactivatedUserRegistry(userRepository);
        registry.run(null);

        UUID justDeactivated = UUID.randomUUID();
        assertThat(registry.isRevoked(justDeactivated)).isFalse();
        registry.markDeactivated(justDeactivated);
        assertThat(registry.isRevoked(justDeactivated)).isTrue();
    }

    @Test
    void isRevoked_nullSafe() {
        DeactivatedUserRegistry registry = new DeactivatedUserRegistry(userRepository);
        assertThat(registry.isRevoked(null)).isFalse();
    }
}
