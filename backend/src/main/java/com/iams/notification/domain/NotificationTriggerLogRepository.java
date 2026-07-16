package com.iams.notification.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTriggerLogRepository extends JpaRepository<NotificationTriggerLog, UUID> {

    boolean existsByEventTypeAndEntityIdAndThresholdKey(NotificationEventType eventType, UUID entityId,
                                                        String thresholdKey);
}
