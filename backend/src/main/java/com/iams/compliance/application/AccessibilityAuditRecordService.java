package com.iams.compliance.application;

import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.AccessibilityAuditOutcome;
import com.iams.compliance.domain.AccessibilityAuditRecord;
import com.iams.compliance.domain.AccessibilityAuditRecordRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-CMP-04: the date/outcome of the latest WCAG 2.1 AA audit. Single row -
 * {@link #record} always updates that one row rather than appending a new
 * one, since the story only asks for "the latest" audit's status to be
 * visible, not a full history of every prior audit.
 */
@Service
public class AccessibilityAuditRecordService {

    private final AccessibilityAuditRecordRepository recordRepository;
    private final CurrentUserProvider currentUserProvider;

    public AccessibilityAuditRecordService(AccessibilityAuditRecordRepository recordRepository, CurrentUserProvider currentUserProvider) {
        this.recordRepository = recordRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Optional<AccessibilityAuditRecord> current() {
        return recordRepository.findAll().stream().findFirst();
    }

    @Transactional
    public AccessibilityAuditRecord record(LocalDate auditDate, AccessibilityAuditOutcome outcome, String notes) {
        AccessibilityAuditRecord record = current().orElseGet(AccessibilityAuditRecord::new);
        UUID actor = currentUserProvider.current().id();
        boolean isNew = record.getId() == null;
        record.setAuditDate(auditDate);
        record.setOutcome(outcome);
        record.setNotes(notes);
        if (isNew) {
            record.setCreatedBy(actor);
        } else {
            record.setUpdatedBy(actor);
        }
        return recordRepository.save(record);
    }
}
