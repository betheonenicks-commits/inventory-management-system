package com.iams.compliance.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.RetentionEntityType;
import com.iams.compliance.domain.RetentionExpiryAction;
import com.iams.compliance.domain.RetentionPolicy;
import com.iams.compliance.domain.RetentionPolicyRepository;
import com.iams.org.domain.Person;
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
 * §5.4's named floors enforced at save time. Two entity types have a real,
 * executable purge: {@link RetentionEntityType#SECURITY_EVENT_LOG} (DELETE old
 * log rows) and {@link RetentionEntityType#PERSON} (ANONYMIZE departed persons
 * past retention, reusing the hold-aware PersonAnonymizationService). The
 * remaining three (DISPOSED_ASSET/ASSET_HISTORY_EVENT/AUDIT_RECORD) can be
 * configured and floor-validated but have no executable purge - deleting those
 * rows means cascading across EPIC-AST/LIF/AUD's own tables (append-only
 * evidence, per BRD §5.3), a materially larger and riskier change than this
 * story scopes. Documented as a real, narrower-than-the-full-catalog scope,
 * not a silently incomplete purge.
 * <p>
 * US-CMP-06: the PERSON purge is legal-hold-aware for free - each anonymize()
 * throws 423 when a linked audit is under an active hold, and the purge loop
 * catches that and skips the person rather than aborting the whole run
 * (AC-CMP-01-H's "only rows ... not under hold are purged").
 */
@Service
public class RetentionPolicyService {

    private static final Map<RetentionEntityType, Integer> FLOOR_DAYS = Map.of(
            RetentionEntityType.SECURITY_EVENT_LOG, 7 * 365,
            RetentionEntityType.AUDIT_RECORD, 7 * 365,      // BRD §5.4: "security/audit logs 7 years"
            RetentionEntityType.DISPOSED_ASSET, 3 * 365
    );

    private final RetentionPolicyRepository policyRepository;
    private final SecurityEventLogRepository securityEventLogRepository;
    private final SecurityEventLogger securityEventLogger;
    private final CurrentUserProvider currentUserProvider;
    private final PersonAnonymizationService personAnonymizationService;

    public RetentionPolicyService(RetentionPolicyRepository policyRepository, SecurityEventLogRepository securityEventLogRepository,
                                   SecurityEventLogger securityEventLogger, CurrentUserProvider currentUserProvider,
                                   PersonAnonymizationService personAnonymizationService) {
        this.policyRepository = policyRepository;
        this.securityEventLogRepository = securityEventLogRepository;
        this.securityEventLogger = securityEventLogger;
        this.currentUserProvider = currentUserProvider;
        this.personAnonymizationService = personAnonymizationService;
    }

    /** US-CMP-01: the outcome of a purge run - how many rows it acted on, and how many it left in place (e.g. under a legal hold). */
    public record PurgeResult(RetentionEntityType entityType, long purged, long skipped, String detail) {
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

    /** Back-compat entry for the security-event-log purge endpoint; returns just the deleted count. */
    public long runPurge() {
        return purge(RetentionEntityType.SECURITY_EVENT_LOG).purged();
    }

    /**
     * US-CMP-01 AC-H: run the configured purge for one entity type - "only rows older than
     * [the period] and not under hold are purged, and the purge itself is logged." Deliberately
     * NOT @Transactional at this level: the PERSON path delegates each anonymize to its own
     * transaction (via the PersonAnonymizationService bean) so one hold-blocked person's 423
     * doesn't poison a run over hundreds of others.
     */
    public PurgeResult purge(RetentionEntityType entityType) {
        RetentionPolicy policy = policyRepository.findByEntityType(entityType)
                .orElseThrow(() -> ValidationFailedException.singleField("entityType",
                        "No retention policy is configured for " + entityType));
        return switch (entityType) {
            case SECURITY_EVENT_LOG -> purgeSecurityEventLog(policy);
            case PERSON -> purgePersons(policy);
            default -> throw ValidationFailedException.singleField("entityType",
                    entityType + " has a configurable policy but no executable purge yet - its records live in "
                            + "append-only evidence tables whose deletion is out of scope here");
        };
    }

    private PurgeResult purgeSecurityEventLog(RetentionPolicy policy) {
        if (policy.getExpiryAction() != RetentionExpiryAction.DELETE) {
            throw ValidationFailedException.singleField("expiryAction",
                    "SECURITY_EVENT_LOG's configured expiry action is " + policy.getExpiryAction() + ", not DELETE - purge refused");
        }
        Instant cutoff = Instant.now().minus(policy.getRetentionPeriodDays(), ChronoUnit.DAYS);
        long deleted = securityEventLogRepository.deleteByCreatedAtBefore(cutoff);
        // Security event logs have no legal-hold scope (holds are ASSET/AUDIT), so nothing to skip here.
        String detail = "Purged " + deleted + " security_event_log rows older than " + cutoff;
        securityEventLogger.record(SecurityEventType.RETENTION_PURGE_EXECUTED, currentUserProvider.current().id(), null, null, detail);
        return new PurgeResult(RetentionEntityType.SECURITY_EVENT_LOG, deleted, 0, detail);
    }

    /**
     * US-CMP-01 + US-CMP-06: anonymize departed persons whose record has been untouched longer
     * than the retention period, skipping any the hold-aware anonymize() refuses - a legal hold
     * on one of the person's linked audits (423) or an outstanding asset assignment (409). Those
     * are left intact, exactly the "not under hold are purged" guarantee, rather than aborting.
     */
    private PurgeResult purgePersons(RetentionPolicy policy) {
        if (policy.getExpiryAction() != RetentionExpiryAction.ANONYMIZE) {
            throw ValidationFailedException.singleField("expiryAction",
                    "PERSON's configured expiry action is " + policy.getExpiryAction() + ", not ANONYMIZE - purge refused");
        }
        // No explicit deactivation timestamp exists; a departed person's record isn't touched
        // afterwards, so updatedAt is the best available "departed since" proxy.
        Instant cutoff = Instant.now().minus(policy.getRetentionPeriodDays(), ChronoUnit.DAYS);
        long anonymized = 0;
        long skipped = 0;
        for (Person person : personAnonymizationService.eligible()) {
            Instant departedSince = person.getUpdatedAt() != null ? person.getUpdatedAt() : person.getCreatedAt();
            if (departedSince == null || departedSince.isAfter(cutoff)) {
                continue; // not past retention yet
            }
            try {
                personAnonymizationService.anonymize(person.getId());
                anonymized++;
            } catch (LegalHoldActiveException | ConflictException e) {
                // CMP-06: a linked audit is under an active legal hold (423), or the person still
                // has an outstanding asset assignment (409) - either way, left intact this run.
                skipped++;
            }
        }
        String detail = "Anonymized " + anonymized + " departed person(s) past retention; skipped " + skipped
                + " (legal hold on a linked audit, or outstanding asset assignment)";
        securityEventLogger.record(SecurityEventType.RETENTION_PURGE_EXECUTED, currentUserProvider.current().id(), null, null, detail);
        return new PurgeResult(RetentionEntityType.PERSON, anonymized, skipped, detail);
    }
}
