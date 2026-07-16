package com.iams.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.notification.domain.Notification;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationPreferenceRepository;
import com.iams.notification.domain.NotificationRepository;
import java.time.Instant;
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
class NotificationQueryServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private NotificationQueryService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new NotificationQueryService(notificationRepository, preferenceRepository, currentUserProvider);
        userId = UUID.randomUUID();
        // lenient: the locked-type test rejects before ever consulting the provider
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(userId, "emp", Set.of("EMPLOYEE_VOLUNTEER"), Set.of()));
    }

    @Test
    void markRead_isNotFoundForAnotherUsersNotification() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void markRead_isIdempotent() {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setReadAt(Instant.parse("2026-07-16T00:00:00Z"));
        when(notificationRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(n));

        Notification result = service.markRead(UUID.randomUUID());

        // Already read: the original timestamp is preserved, no rewrite.
        assertThat(result.getReadAt()).isEqualTo(Instant.parse("2026-07-16T00:00:00Z"));
    }

    @Test
    void updatePreference_refusesAdminLockedTypes() {
        assertThatThrownBy(() -> service.updatePreference(NotificationEventType.SECURITY_ALERT, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void preferences_showEveryCatalogTypeWithLockFlags() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(List.of());

        List<NotificationQueryService.PreferenceView> views = service.preferences();

        assertThat(views).hasSize(NotificationEventType.values().length);
        assertThat(views.stream().filter(NotificationQueryService.PreferenceView::locked))
                .extracting(NotificationQueryService.PreferenceView::eventType)
                .containsExactlyInAnyOrder(NotificationEventType.SECURITY_ALERT,
                        NotificationEventType.PENDING_APPROVAL);
        // Defaults: email on everywhere until a row says otherwise.
        assertThat(views).allMatch(NotificationQueryService.PreferenceView::emailEnabled);
    }

    @Test
    void updatePreference_savesOptOutForUnlockedType() {
        when(preferenceRepository.findByUserIdAndEventType(userId, NotificationEventType.LOW_STOCK))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationQueryService.PreferenceView view =
                service.updatePreference(NotificationEventType.LOW_STOCK, false);

        assertThat(view.emailEnabled()).isFalse();
        verify(preferenceRepository).save(any());
    }
}
