package com.iams.notification.application;

import com.iams.notification.domain.Notification;
import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationDelivery;
import com.iams.notification.domain.NotificationDeliveryRepository;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationPreferenceRepository;
import com.iams.notification.domain.NotificationRepository;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one entry point every event producer calls. Writes rows only - the
 * in-app notification (always, US-NTF-03: the channel that cannot fail) and
 * a PENDING email delivery where the user's preference (or an admin lock)
 * says so. Actual sending happens in NotificationDeliveryJob's sweep, so a
 * slow or down SMTP server can never stall or roll back the business
 * transaction that raised the event.
 */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateService templateService;
    private final AppUserRepository userRepository;
    private final NotificationProperties properties;

    public NotificationDispatchService(NotificationRepository notificationRepository,
                                       NotificationDeliveryRepository deliveryRepository,
                                       NotificationPreferenceRepository preferenceRepository,
                                       NotificationTemplateService templateService, AppUserRepository userRepository,
                                       NotificationProperties properties) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.preferenceRepository = preferenceRepository;
        this.templateService = templateService;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional
    public void dispatch(NotificationEventType eventType, Collection<UUID> recipientUserIds,
                         Map<String, String> variables, String resourcePath) {
        for (UUID userId : new LinkedHashSet<>(recipientUserIds)) {
            if (userId == null) {
                continue;
            }
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Notification {} skipped for unknown user {}", eventType, userId);
                continue;
            }

            NotificationTemplateService.Rendered inApp =
                    templateService.render(eventType, NotificationChannel.IN_APP, variables);
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setEventType(eventType);
            notification.setTitle(inApp.subject());
            notification.setBody(inApp.body());
            notification.setResourcePath(resourcePath);
            notification = notificationRepository.save(notification);

            if (emailWanted(eventType, userId) && user.getEmail() != null && !user.getEmail().isBlank()) {
                NotificationTemplateService.Rendered email =
                        templateService.render(eventType, NotificationChannel.EMAIL, variables);
                NotificationDelivery delivery = new NotificationDelivery();
                delivery.setNotification(notification);
                delivery.setChannel(NotificationChannel.EMAIL);
                delivery.setRenderedSubject(email.subject());
                delivery.setRenderedBody(email.body());
                deliveryRepository.save(delivery);
            }
        }
    }

    /** Locked types always email (US-NTF-05's admin-mandatory rule); otherwise the user's preference, defaulting on. */
    private boolean emailWanted(NotificationEventType eventType, UUID userId) {
        if (!properties.isMailEnabled()) {
            return false;
        }
        if (eventType.locked()) {
            return true;
        }
        return preferenceRepository.findByUserIdAndEventType(userId, eventType)
                .map(p -> p.isEmailEnabled())
                .orElse(true);
    }
}
