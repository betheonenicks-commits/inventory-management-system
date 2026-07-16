package com.iams.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.notification.domain.Notification;
import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationDelivery;
import com.iams.notification.domain.NotificationDeliveryRepository;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.infrastructure.EmailSender;
import com.iams.notification.infrastructure.SmsGateway;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryJobTest {

    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private EmailSender emailSender;
    @Mock private SmsGateway smsGateway;
    @Mock private NotificationDispatchService dispatchService;
    @Mock private RecipientResolverService recipientResolver;

    private NotificationDeliveryJob job;
    private UUID userId;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        job = new NotificationDeliveryJob(deliveryRepository, userRepository, emailSender, smsGateway, properties,
                dispatchService, recipientResolver);
        userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setEmail("holder@x.org");
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private NotificationDelivery delivery(NotificationEventType eventType, NotificationChannel channel, int attempts) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEventType(eventType);
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(notification);
        delivery.setChannel(channel);
        delivery.setRenderedSubject("s");
        delivery.setRenderedBody("b");
        delivery.setAttempts(attempts);
        return delivery;
    }

    @Test
    void attempt_marksSentOnSuccess() {
        NotificationDelivery d = delivery(NotificationEventType.LOW_STOCK, NotificationChannel.EMAIL, 0);

        job.attempt(d);

        verify(emailSender).send(eq("holder@x.org"), eq("s"), eq("b"));
        assertThat(d.getStatus()).isEqualTo(NotificationDelivery.Status.SENT);
        assertThat(d.getSentAt()).isNotNull();
    }

    @Test
    void attempt_schedulesExponentialBackoffOnFailure() {
        doThrow(new RuntimeException("smtp down")).when(emailSender).send(anyString(), anyString(), anyString());
        NotificationDelivery d = delivery(NotificationEventType.LOW_STOCK, NotificationChannel.EMAIL, 0);
        Instant before = Instant.now();

        job.attempt(d);

        assertThat(d.getStatus()).isEqualTo(NotificationDelivery.Status.FAILED);
        assertThat(d.getAttempts()).isEqualTo(1);
        assertThat(d.getLastError()).contains("smtp down");
        // First retry: base 60s after now.
        assertThat(d.getNextAttemptAt()).isAfterOrEqualTo(before.plusSeconds(59));
    }

    @Test
    void attempt_exhaustsAtCapAndEscalatesApprovalClassToAdmins() {
        doThrow(new RuntimeException("bounce")).when(emailSender).send(anyString(), anyString(), anyString());
        when(recipientResolver.admins()).thenReturn(Set.of(UUID.randomUUID()));
        NotificationDelivery d = delivery(NotificationEventType.PENDING_APPROVAL, NotificationChannel.EMAIL, 2);

        job.attempt(d);

        assertThat(d.getStatus()).isEqualTo(NotificationDelivery.Status.EXHAUSTED);
        // US-NTF-08: a stranded approval raises an in-app alert to administrators.
        verify(dispatchService).dispatch(eq(NotificationEventType.SECURITY_ALERT), any(), any(), any());
    }

    @Test
    void attempt_exhaustsNonApprovalClassSilently() {
        doThrow(new RuntimeException("bounce")).when(emailSender).send(anyString(), anyString(), anyString());
        NotificationDelivery d = delivery(NotificationEventType.LOW_STOCK, NotificationChannel.EMAIL, 2);

        job.attempt(d);

        assertThat(d.getStatus()).isEqualTo(NotificationDelivery.Status.EXHAUSTED);
        verify(dispatchService, org.mockito.Mockito.never()).dispatch(any(), any(), any(), any());
    }

    @Test
    void attempt_smsWithoutGatewayDegradesGracefully() {
        when(smsGateway.configured()).thenReturn(false);
        NotificationDelivery d = delivery(NotificationEventType.SECURITY_ALERT, NotificationChannel.SMS, 0);

        job.attempt(d);

        // US-NTF-02: nothing errors, no pointless retries - in-app/email already carried it.
        assertThat(d.getStatus()).isEqualTo(NotificationDelivery.Status.EXHAUSTED);
        assertThat(d.getLastError()).contains("No SMS gateway configured");
    }
}
