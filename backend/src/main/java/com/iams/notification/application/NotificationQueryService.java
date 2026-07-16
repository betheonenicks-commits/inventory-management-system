package com.iams.notification.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.notification.domain.Notification;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationPreference;
import com.iams.notification.domain.NotificationPreferenceRepository;
import com.iams.notification.domain.NotificationRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-NTF-03/05: a user's own notifications and channel preferences.
 * Own-rows-only throughout - another user's notification id is a 404, the
 * same isolation discipline saved searches and export jobs follow.
 */
@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationQueryService(NotificationRepository notificationRepository,
                                    NotificationPreferenceRepository preferenceRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(boolean unreadOnly, int page, int size) {
        UUID userId = currentUserProvider.current().id();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        return unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadAtIsNull(currentUserProvider.current().id());
    }

    /** Idempotent: marking an already-read notification read again is a no-op, not an error. */
    @Transactional
    public Notification markRead(UUID id) {
        Notification notification = notificationRepository
                .findByIdAndUserId(id, currentUserProvider.current().id())
                .orElseThrow(() -> NotFoundException.of("Notification", id));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.saveAndFlush(notification);
        }
        return notification;
    }

    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(currentUserProvider.current().id());
    }

    /** Every catalog type with the caller's effective email setting and whether an admin lock pins it (US-NTF-05). */
    @Transactional(readOnly = true)
    public List<PreferenceView> preferences() {
        UUID userId = currentUserProvider.current().id();
        Map<NotificationEventType, NotificationPreference> saved = preferenceRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(NotificationPreference::getEventType, p -> p));
        return Arrays.stream(NotificationEventType.values())
                .map(type -> new PreferenceView(type,
                        type.locked() || saved.get(type) == null || saved.get(type).isEmailEnabled(),
                        type.locked()))
                .toList();
    }

    @Transactional
    public PreferenceView updatePreference(NotificationEventType eventType, boolean emailEnabled) {
        if (eventType.locked()) {
            throw new ConflictException("PREFERENCE_LOCKED",
                    "The administrator has locked " + eventType + " as mandatory - its email channel cannot be turned off");
        }
        UUID userId = currentUserProvider.current().id();
        NotificationPreference preference = preferenceRepository.findByUserIdAndEventType(userId, eventType)
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setUserId(userId);
                    created.setEventType(eventType);
                    return created;
                });
        preference.setEmailEnabled(emailEnabled);
        preferenceRepository.save(preference);
        return new PreferenceView(eventType, emailEnabled, false);
    }

    public record PreferenceView(NotificationEventType eventType, boolean emailEnabled, boolean locked) {
    }
}
