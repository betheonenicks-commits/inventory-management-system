package com.iams.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findFirstByEventTypeAndChannelOrderByVersionDesc(
            NotificationEventType eventType, NotificationChannel channel);

    List<NotificationTemplate> findByEventTypeAndChannelOrderByVersionDesc(
            NotificationEventType eventType, NotificationChannel channel);

    List<NotificationTemplate> findAllByOrderByEventTypeAscChannelAscVersionDesc();
}
