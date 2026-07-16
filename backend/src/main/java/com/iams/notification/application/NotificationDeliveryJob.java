package com.iams.notification.application;

import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationDelivery;
import com.iams.notification.domain.NotificationDeliveryRepository;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.infrastructure.EmailSender;
import com.iams.notification.infrastructure.SmsGateway;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-NTF-08: works the delivery queue. Each due PENDING/FAILED delivery is
 * attempted; failure logs the error on the row and schedules the next try
 * with exponential backoff (base 60s: +1m, +2m, +4m). After the attempt cap,
 * the delivery is EXHAUSTED - and for approval-class events an in-app alert
 * raises to the administrators, so a bounced email never silently strands an
 * approval.
 */
@Component
public class NotificationDeliveryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryJob.class);

    private final NotificationDeliveryRepository deliveryRepository;
    private final AppUserRepository userRepository;
    private final EmailSender emailSender;
    private final SmsGateway smsGateway;
    private final NotificationProperties properties;
    private final NotificationDispatchService dispatchService;
    private final RecipientResolverService recipientResolver;

    public NotificationDeliveryJob(NotificationDeliveryRepository deliveryRepository,
                                   AppUserRepository userRepository, EmailSender emailSender, SmsGateway smsGateway,
                                   NotificationProperties properties, NotificationDispatchService dispatchService,
                                   RecipientResolverService recipientResolver) {
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.smsGateway = smsGateway;
        this.properties = properties;
        this.dispatchService = dispatchService;
        this.recipientResolver = recipientResolver;
    }

    @Scheduled(fixedDelayString = "${iams.notifications.delivery-sweep-ms:30000}")
    @Transactional
    public int processDue() {
        List<NotificationDelivery> due = deliveryRepository.findDue(Instant.now());
        int processed = 0;
        for (NotificationDelivery delivery : due) {
            attempt(delivery);
            processed++;
        }
        return processed;
    }

    void attempt(NotificationDelivery delivery) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        try {
            AppUser user = userRepository.findById(delivery.getNotification().getUserId()).orElseThrow();
            if (delivery.getChannel() == NotificationChannel.EMAIL) {
                emailSender.send(user.getEmail(), delivery.getRenderedSubject(), delivery.getRenderedBody());
            } else if (delivery.getChannel() == NotificationChannel.SMS) {
                if (!smsGateway.configured()) {
                    // US-NTF-02: no gateway is not an error - degrade gracefully and stop retrying.
                    delivery.setStatus(NotificationDelivery.Status.EXHAUSTED);
                    delivery.setLastError("No SMS gateway configured - degraded to email/in-app");
                    deliveryRepository.save(delivery);
                    return;
                }
                smsGateway.send(user.getEmail(), delivery.getRenderedBody());
            }
            delivery.setStatus(NotificationDelivery.Status.SENT);
            delivery.setSentAt(Instant.now());
            delivery.setLastError(null);
        } catch (Exception e) {
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            delivery.setLastError(error.length() > 500 ? error.substring(0, 500) : error);
            if (delivery.getAttempts() >= properties.getMaxAttempts()) {
                delivery.setStatus(NotificationDelivery.Status.EXHAUSTED);
                escalateIfApprovalClass(delivery);
            } else {
                delivery.setStatus(NotificationDelivery.Status.FAILED);
                long delaySeconds = properties.getBackoffBaseSeconds() * (1L << (delivery.getAttempts() - 1));
                delivery.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
            }
            log.warn("Notification delivery {} attempt {} failed: {}", delivery.getId(), delivery.getAttempts(),
                    delivery.getLastError());
        }
        deliveryRepository.save(delivery);
    }

    private void escalateIfApprovalClass(NotificationDelivery delivery) {
        NotificationEventType eventType = delivery.getNotification().getEventType();
        if (!eventType.approvalClass()) {
            return;
        }
        dispatchService.dispatch(NotificationEventType.SECURITY_ALERT, recipientResolver.admins(),
                Map.of("summary", "Notification delivery exhausted for an approval-class event",
                        "detail", "All " + properties.getMaxAttempts() + " delivery attempts failed for a "
                                + eventType + " notification (delivery " + delivery.getId()
                                + "). The approval may be stranded - check the recipient's email address. Last error: "
                                + delivery.getLastError()),
                "/notifications");
    }
}
