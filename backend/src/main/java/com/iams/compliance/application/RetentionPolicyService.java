package com.iams.compliance.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.RetentionEntityType;
import com.iams.compliance.domain.RetentionExpiryAction;
import com.iams.compliance.domain.RetentionPolicy;
import com.iams.compliance.domain.RetentionPolicyRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-CMP-01: per-entity-type retention period + expiry action, with BRD
 * §5.4's named floors enforced at save time. Only {@link RetentionEntityType#SECURITY_EVENT_LOG}
 * has a real, executable purge in this codebase today ({@link #runPurge()}) -
 * the other entity types can have a policy configured and validated against
 * its floor, but nothing yet reads DISPOSED_ASSET/PERSON/ASSET_HISTORY_EVENT/
 * AUDIT_RECORD policies to actually act on them (that would mean touching
 * EPIC-AST/LIF/AUD's own retention semantics, real scope beyond configuring
 * the policy itself). Documented as a real, narrower-than-the-full-AC scope,
 * not a silently incomplete purge.
 */
@Service
public class RetentionPolicyService {

    private static final Map<RetentionEntityType, Integer> FLOOR_DAYS = Map.of(
            RetentionEntityType.SECURITY_EVENT_LOG, 7 * 365,
            RetentionEntityType.DISPOSED_ASSET, 3 * 365
    );

    private final RetentionPolicyRepository policyRepository;
    private final SecurityEventLogRepository securityEventLogRepository;
    private final SecurityEventLogger securityEventLogger;
    private final CurrentUserProvider currentUserProvider;

    public RetentionPolicyService(RetentionPolicyRepository policyRepository, SecurityEventLogRepository securityEventLogRepository,
                                   SecurityEventLogger securityEventLogger, CurrentUserProvider currentUserProvider) {
        this.policyRepository = policyRepository;
        this.securityEventLogRepository = securityEventLogRepository;
        this.securityEventLogger = securityEventLogger;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public RetentionPolicy save(RetentionEntityType entityType, int retentionPeriodDays, RetentionExpiryAction expiryAction) {
        Integer floor = FLOOR_DAYS.get(entityType);
        if (floor != null && retentionPeriodDays < floor) {
            // AC-CMP-01-X: rejected citing the floor.
            throw ValidationFailedException.singleField("retentionPeriodDays",
                    "Must be at least " + floor + " days for " + entityType + " (BRD §5.4 floor)");
        }
        if (retentionPeriodDays <= 0) {
            throw ValidationFailedException.singleField("retentionPeriodDays", "Must be a positive number of days");
        }

        RetentionPolicy policy = policyRepository.findByEntityType(entityType).orElseGet(RetentionPolicy::new);
        UUID actor = currentUserProvider.current().id();
        boolean isNew = policy.getId() == null;
        policy.setEntityType(entityType);
        policy.setRetentionPeriodDays(retentionPeriodDays);
        policy.setExpiryAction(expiryAction);
        if (isNew) {
            policy.setCreatedBy(actor);
        } else {
            policy.setUpdatedBy(actor);
        }
        return policyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public RetentionPolicy get(UUID id) {
        return policyRepository.findById(id).orElseThrow(() -> NotFoundException.of("RetentionPolicy", id));
    }

    @Transactional(readOnly = true)
    public List<RetentionPolicy> list() {
        return policyRepository.findAll();
    }

    /**
     * US-CMP-01 AC-H: "the policy engine runs, then only rows older than [the
     * period] and not under hold are purged, and the purge itself is
     * logged." Security event logs have no legal-hold concept in this
     * codebase (BRD §5.4 names holds against personal data / disposed-asset /
     * audit records, not the security log itself), so there's no hold check
     * to apply here.
     */
    @Transactional
    public long runPurge() {
        RetentionPolicy policy = policyRepository.findByEntityType(RetentionEntityType.SECURITY_EVENT_LOG)
                .orElseThrow(() -> new IllegalStateException("No retention policy configured for SECURITY_EVENT_LOG"));
        if (policy.getExpiryAction() != RetentionExpiryAction.DELETE) {
            throw ValidationFailedException.singleField("expiryAction",
                    "SECURITY_EVENT_LOG's configured expiry action is " + policy.getExpiryAction() + ", not DELETE - purge refused");
        }
        Instant cutoff = Instant.now().minus(policy.getRetentionPeriodDays(), ChronoUnit.DAYS);
        long deleted = securityEventLogRepository.deleteByCreatedAtBefore(cutoff);
        securityEventLogger.record(SecurityEventType.RETENTION_PURGE_EXECUTED, currentUserProvider.current().id(), null, null,
                "Purged " + deleted + " security_event_log rows older than " + cutoff);
        return deleted;
    }
}
