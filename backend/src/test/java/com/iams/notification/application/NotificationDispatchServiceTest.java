package com.iams.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.notification.domain.Notification;
import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationDelivery;
import com.iams.notification.domain.NotificationDeliveryRepository;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationPreference;
import com.iams.notification.domain.NotificationPreferenceRepository;
import com.iams.notification.domain.NotificationRepository;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private NotificationTemplateService templateService;
    @Mock private AppUserRepository userRepository;

    private NotificationDispatchService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new NotificationDispatchService(notificationRepository, deliveryRepository, preferenceRepository,
                templateService, userRepository, new NotificationProperties());
        userId = UUID.randomUUID();
        lenient().when(templateService.render(any(), eq(NotificationChannel.IN_APP), any()))
                .thenReturn(new NotificationTemplateService.Rendered("in-app subject", "in-app body", 1));
        lenient().when(templateService.render(any(), eq(NotificationChannel.EMAIL), any()))
                .thenReturn(new NotificationTemplateService.Rendered("email subject", "email body", 1));
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        return user;
    }

    @Test
    void dispatch_alwaysWritesInAppRowAndQueuesEmailByDefault() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("u@x.org")));
        when(preferenceRepository.findByUserIdAndEventType(userId, NotificationEventType.LOW_STOCK))
                .thenReturn(Optional.empty());

        service.dispatch(NotificationEventType.LOW_STOCK, List.of(userId), Map.of(), "/inventory");

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());
        assertThat(notification.getValue().getTitle()).isEqualTo("in-app subject");
        assertThat(notification.getValue().getResourcePath()).isEqualTo("/inventory");

        ArgumentCaptor<NotificationDelivery> delivery = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository).save(delivery.capture());
        assertThat(delivery.getValue().getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(delivery.getValue().getRenderedSubject()).isEqualTo("email subject");
    }

    @Test
    void dispatch_respectsEmailOptOutButStillWritesInApp() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("u@x.org")));
        NotificationPreference off = new NotificationPreference();
        off.setEmailEnabled(false);
        when(preferenceRepository.findByUserIdAndEventType(userId, NotificationEventType.LOW_STOCK))
                .thenReturn(Optional.of(off));

        service.dispatch(NotificationEventType.LOW_STOCK, List.of(userId), Map.of(), null);

        verify(notificationRepository).save(any(Notification.class));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void dispatch_ignoresOptOutForAdminLockedTypes() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("u@x.org")));

        service.dispatch(NotificationEventType.SECURITY_ALERT, List.of(userId), Map.of(), null);

        // Locked types never even consult the preference table.
        verify(preferenceRepository, never()).findByUserIdAndEventType(any(), any());
        verify(deliveryRepository).save(any(NotificationDelivery.class));
    }

    @Test
    void dispatch_skipsEmailWhenUserHasNoAddress() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(null)));

        service.dispatch(NotificationEventType.SECURITY_ALERT, List.of(userId), Map.of(), null);

        verify(notificationRepository).save(any(Notification.class));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void dispatch_deduplicatesRecipientsAndSkipsUnknownUsers() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("u@x.org")));
        UUID ghost = UUID.randomUUID();
        when(userRepository.findById(ghost)).thenReturn(Optional.empty());

        service.dispatch(NotificationEventType.SECURITY_ALERT, List.of(userId, userId, ghost), Map.of(), null);

        verify(notificationRepository).save(any(Notification.class));
    }
}
