package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingCorrection;
import com.iams.audit.domain.AuditFindingCorrectionRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.CorrectionField;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditFindingCorrectionServiceTest {

    @Mock private AuditFindingRepository findingRepository;
    @Mock private AuditFindingCorrectionRepository correctionRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AuditService auditService;

    private AuditFindingCorrectionService service;
    private UUID actorId;
    private UUID auditId;
    private UUID findingId;
    private Audit audit;
    private AuditFinding finding;

    @BeforeEach
    void setUp() {
        service = new AuditFindingCorrectionService(findingRepository, correctionRepository, currentUserProvider,
                auditService);
        actorId = UUID.randomUUID();
        auditId = UUID.randomUUID();
        findingId = UUID.randomUUID();
        audit = new Audit();
        audit.setId(auditId);
        finding = new AuditFinding();
        finding.setId(findingId);
        finding.setAudit(audit);
        finding.setCondition(AssetCondition.FAIR);
        finding.setRemarks("Original remark");
        org.mockito.Mockito.lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "admin", Set.of("ADMIN")));
    }

    @Test
    void correct_capturesOriginalValueAsOldValue_whenNoPriorCorrections() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));
        when(correctionRepository.findByFindingIdOrderByCreatedAtAsc(findingId)).thenReturn(List.of());
        when(correctionRepository.save(any(AuditFindingCorrection.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditFindingCorrection result = service.correct(auditId, findingId, CorrectionField.CONDITION, "MAJOR_DAMAGE");

        assertThat(result.getOldValue()).isEqualTo("FAIR");
        assertThat(result.getNewValue()).isEqualTo("MAJOR_DAMAGE");
        assertThat(result.getActorId()).isEqualTo(actorId);
        // AC-AUD-24-H: the original finding itself is never touched.
        assertThat(finding.getCondition()).isEqualTo(AssetCondition.FAIR);
    }

    @Test
    void correct_capturesPriorCorrectionAsOldValue_whenOneAlreadyExists() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));
        AuditFindingCorrection priorCorrection = new AuditFindingCorrection();
        priorCorrection.setFieldName(CorrectionField.REMARKS);
        priorCorrection.setNewValue("First correction");
        when(correctionRepository.findByFindingIdOrderByCreatedAtAsc(findingId)).thenReturn(List.of(priorCorrection));
        when(correctionRepository.save(any(AuditFindingCorrection.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditFindingCorrection result = service.correct(auditId, findingId, CorrectionField.REMARKS, "Second correction");

        assertThat(result.getOldValue()).isEqualTo("First correction");
        assertThat(result.getNewValue()).isEqualTo("Second correction");
    }

    @Test
    void correct_rejectsBlankNewValue() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));

        assertThatThrownBy(() -> service.correct(auditId, findingId, CorrectionField.REMARKS, "  "))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void correct_throwsNotFound_whenFindingBelongsToDifferentAudit() {
        Audit otherAudit = new Audit();
        otherAudit.setId(UUID.randomUUID());
        finding.setAudit(otherAudit);
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));

        assertThatThrownBy(() -> service.correct(auditId, findingId, CorrectionField.REMARKS, "New value"))
                .isInstanceOf(NotFoundException.class);
    }

    // US-AUD-11/24 fix: getFinding() - the single funnel for corrections AND evidence
    // upload/list/delete - now enforces org-scope instead of silently skipping it.
    @Test
    void getFinding_refusesAnOutOfScopeFinding() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("out of scope"))
                .when(auditService).requireInScope(audit);

        assertThatThrownBy(() -> service.getFinding(auditId, findingId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getFinding_callsTheScopeCheck_forAnInScopeFinding() {
        when(findingRepository.findByIdWithAsset(findingId)).thenReturn(Optional.of(finding));

        AuditFinding result = service.getFinding(auditId, findingId);

        assertThat(result).isSameAs(finding);
        org.mockito.Mockito.verify(auditService).requireInScope(audit);
    }
}
