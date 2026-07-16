package com.iams.notification.application;

import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationTriggerLog;
import com.iams.notification.domain.NotificationTriggerLogRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-NTF-06's exactly-once gate, isolated in its own component so the
 * REQUIRES_NEW transaction actually goes through the proxy (a self-invoked
 * @Transactional method would silently run in the caller's transaction, and
 * a lost insert race would poison it - the same UnexpectedRollbackException
 * class of bug the batch-label service hit).
 */
@Service
public class TriggerClaimService {

    private final NotificationTriggerLogRepository repository;

    public TriggerClaimService(NotificationTriggerLogRepository repository) {
        this.repository = repository;
    }

    /** True exactly once per (event, entity, threshold): the DB unique constraint decides the winner. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(NotificationEventType eventType, UUID entityId, String thresholdKey) {
        if (repository.existsByEventTypeAndEntityIdAndThresholdKey(eventType, entityId, thresholdKey)) {
            return false;
        }
        try {
            NotificationTriggerLog log = new NotificationTriggerLog();
            log.setEventType(eventType);
            log.setEntityId(entityId);
            log.setThresholdKey(thresholdKey);
            repository.saveAndFlush(log);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
