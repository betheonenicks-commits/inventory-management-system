package com.iams.storage.application;

import com.iams.storage.domain.AttachmentRepository;
import com.iams.storage.infrastructure.ObjectStorageClient;
import com.iams.storage.infrastructure.StorageUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US-PLAT-02's orphan reaper - the codebase's first genuinely scheduled job
 * (everything time-based before this, LIF/AUD escalation, is pull-triggered
 * by design; an orphaned binary has no caller to pull it, so a schedule is
 * the honest mechanism here, and the AC names one explicitly).
 *
 * An orphan is an object with no metadata row: the only state
 * AttachmentService's object-first/row-second ordering can leak, produced
 * when the row insert fails after the object committed. The age floor keeps
 * the janitor from racing an in-flight upload (object committed, row commit
 * microseconds away).
 */
@Component
public class AttachmentJanitor {

    private static final Logger log = LoggerFactory.getLogger(AttachmentJanitor.class);

    private final AttachmentRepository repository;
    private final ObjectStorageClient storage;
    private final StorageProperties properties;

    public AttachmentJanitor(AttachmentRepository repository, ObjectStorageClient storage,
                             StorageProperties properties) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${iams.storage.janitor-delay-ms:3600000}",
            initialDelayString = "${iams.storage.janitor-delay-ms:3600000}")
    public void sweep() {
        try {
            int reaped = reapOrphans(Instant.now().minus(Duration.ofHours(properties.getJanitorMinAgeHours())));
            if (reaped > 0) {
                log.info("Attachment janitor reaped {} orphaned object(s)", reaped);
            }
        } catch (StorageUnavailableException e) {
            // A quiet object store must not crash-loop the scheduler thread; next sweep retries.
            log.warn("Attachment janitor skipped a sweep - object store unavailable: {}", e.getMessage());
        }
    }

    /** Package-visible core so tests drive the logic without the scheduler. */
    int reapOrphans(Instant cutoff) {
        List<String> candidates = storage.listKeysOlderThan(cutoff);
        int reaped = 0;
        for (String key : candidates) {
            if (!repository.existsByStorageKey(key)) {
                storage.delete(key);
                reaped++;
            }
        }
        return reaped;
    }
}
