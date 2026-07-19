package com.iams.usr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.UserStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeactivationServiceTest {

    @Mock private AppUserRepository userRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private DeactivatedUserRegistry deactivatedUserRegistry;

    private UserDeactivationService service;

    @BeforeEach
    void setUp() {
        service = new UserDeactivationService(userRepository, assetRepository, currentUserProvider, securityEventLogger,
                refreshTokenService, deactivatedUserRegistry);
    }

    private AppUser userWithPerson(UUID personId, long version) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setVersion(version);
        user.setPersonId(personId);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void deactivate_succeeds_whenNoPersonLinked() {
        AppUser user = userWithPerson(null, 0L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "admin", Set.of("ADMIN")));

        AppUser result = service.deactivate(user.getId(), 0L);

        assertThat(result.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        // US-USR-08: the user's live access tokens are revoked at the next request.
        verify(deactivatedUserRegistry).markDeactivated(user.getId());
        verify(refreshTokenService).revokeAll(user.getId());
    }

    @Test
    void deactivate_succeeds_whenPersonHasNoAssignedAssets() {
        UUID personId = UUID.randomUUID();
        AppUser user = userWithPerson(personId, 0L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of());
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "admin", Set.of("ADMIN")));

        AppUser result = service.deactivate(user.getId(), 0L);

        assertThat(result.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
    }

    @Test
    void deactivate_blocked_whenPersonHasAssignedAssets() {
        UUID personId = UUID.randomUUID();
        AppUser user = userWithPerson(personId, 0L);
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber("AST-2026-000123");
        asset.setName("Dell Latitude 5440");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of(asset));

        assertThatThrownBy(() -> service.deactivate(user.getId(), 0L))
                .isInstanceOfSatisfying(ConflictException.class, ex -> {
                    // Matches IAMS_API_Specification_v1.1.md Section 4.5 exactly: a real client
                    // is coded against errorCode + the structured blockingAssets/resolutionActions
                    // payload, not against parsing asset numbers out of a free-text message.
                    assertThat(ex.getErrorCode()).isEqualTo("USER_HAS_OUTSTANDING_ASSIGNMENTS");
                    assertThat(ex.getTitle()).isEqualTo("Cannot deactivate user with outstanding asset assignments");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> blockingAssets =
                            (List<Map<String, Object>>) ex.getExtraProperties().get("blockingAssets");
                    assertThat(blockingAssets).hasSize(1);
                    assertThat(blockingAssets.get(0)).containsEntry("assetNumber", "AST-2026-000123");
                    assertThat(ex.getExtraProperties()).containsKey("resolutionActions");
                });
        // Blocked deactivation must NOT revoke tokens - the user stays active.
        verify(deactivatedUserRegistry, never()).markDeactivated(user.getId());
    }

    @Test
    void deactivate_rejectsStaleVersion() {
        AppUser user = userWithPerson(null, 3L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.deactivate(user.getId(), 2L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void deactivate_rejectsUnknownUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id, 0L))
                .isInstanceOf(NotFoundException.class);
    }
}
