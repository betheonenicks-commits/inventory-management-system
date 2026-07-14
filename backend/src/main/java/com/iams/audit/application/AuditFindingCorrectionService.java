package com.iams.audit.application;

import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingCorrection;
import com.iams.audit.domain.AuditFindingCorrectionRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.CorrectionField;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-24: the only way a finding's condition or remarks can be corrected -
 * as a new linked row, never an edit to {@link AuditFinding} itself (there is
 * no update/delete endpoint for findings anywhere in this module; that
 * absence is what enforces AC-AUD-24-X, not this service).
 */
@Service
public class AuditFindingCorrectionService {

    private final AuditFindingRepository findingRepository;
    private final AuditFindingCorrectionRepository correctionRepository;
    private final CurrentUserProvider currentUserProvider;

    public AuditFindingCorrectionService(AuditFindingRepository findingRepository,
                                          AuditFindingCorrectionRepository correctionRepository,
                                          CurrentUserProvider currentUserProvider) {
        this.findingRepository = findingRepository;
        this.correctionRepository = correctionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public AuditFindingCorrection correct(UUID auditId, UUID findingId, CorrectionField fieldName, String newValue) {
        AuditFinding finding = getFinding(auditId, findingId);
        if (newValue == null || newValue.isBlank()) {
            throw ValidationFailedException.singleField("newValue", "A new value is required");
        }

        String oldValue = effectiveValue(finding, fieldName, correctionRepository.findByFindingIdOrderByCreatedAtAsc(finding.getId()));
        CurrentUser current = currentUserProvider.current();

        AuditFindingCorrection correction = new AuditFindingCorrection();
        correction.setFinding(finding);
        correction.setFieldName(fieldName);
        correction.setOldValue(oldValue);
        correction.setNewValue(newValue);
        correction.setActorId(current.id());
        correction.setActorUsername(current.username());
        return correctionRepository.save(correction);
    }

    @Transactional(readOnly = true)
    public List<AuditFindingCorrection> corrections(UUID findingId) {
        return correctionRepository.findByFindingIdOrderByCreatedAtAsc(findingId);
    }

    @Transactional(readOnly = true)
    public AuditFinding getFinding(UUID auditId, UUID findingId) {
        AuditFinding finding = findingRepository.findByIdWithAsset(findingId)
                .orElseThrow(() -> NotFoundException.of("AuditFinding", findingId));
        if (!finding.getAudit().getId().equals(auditId)) {
            throw NotFoundException.of("AuditFinding", findingId);
        }
        return finding;
    }

    /**
     * The current value of a field: the newValue of its most recent
     * correction if any exist, else the original recorded value. Also used
     * by AuditFindingMapper to compute the "effective" value shown in
     * responses - kept as one static source of truth rather than duplicated
     * logic in two places.
     */
    public static String effectiveValue(AuditFinding finding, CorrectionField fieldName, List<AuditFindingCorrection> correctionsAscending) {
        for (int i = correctionsAscending.size() - 1; i >= 0; i--) {
            if (correctionsAscending.get(i).getFieldName() == fieldName) {
                return correctionsAscending.get(i).getNewValue();
            }
        }
        return switch (fieldName) {
            case CONDITION -> finding.getCondition() != null ? finding.getCondition().name() : null;
            case REMARKS -> finding.getRemarks();
        };
    }
}
