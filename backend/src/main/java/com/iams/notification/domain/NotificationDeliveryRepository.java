package com.iams.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    @Query("select d from NotificationDelivery d join fetch d.notification "
            + "where d.status in (com.iams.notification.domain.NotificationDelivery.Status.PENDING, "
            + "com.iams.notification.domain.NotificationDelivery.Status.FAILED) "
            + "and d.nextAttemptAt <= :now order by d.nextAttemptAt")
    List<NotificationDelivery> findDue(Instant now);

    List<NotificationDelivery> findByNotificationIdOrderByCreatedAt(UUID notificationId);
}
