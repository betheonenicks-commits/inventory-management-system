package com.iams.audit.application;

import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditExpectedAsset;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.SodWaiver;
import com.iams.usr.domain.SodWaiverRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-09/13/14/22: submitting a completed audit (closing scanning,
 * classifying Missing, signing with password re-authentication, checking for
 * a self-approval SoD conflict) and the Department Head's
 * approve/reject decision. See AuditStatus's Javadoc for why this codebase
 * folds "close scanning" and "approve" into a two-step (not three-step)
 * state machine.
 */
@Service
public class AuditWorkflowService {

    /** Matches the literal scope this project's existing SoD-waiver seed data already uses for audit approval conflicts. */
    private static final String SOD_SCOPE = "AUDIT_APPROVAL";

    private final AuditRepository auditRepository;
    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditFindingRepository findingRepository;
    private final SodWaiverRepository sodWaiverRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public AuditWorkflowService(AuditRepository auditRepository, AuditExpectedAssetRepository expectedAssetRepository,
                                 AuditFindingRepository findingRepository, SodWaiverRepository sodWaiverRepository,
                                 AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                                 CurrentUserProvider currentUserProvider) {
        this.auditRepository = auditRepository;
        this.expectedAssetRepository = expectedAssetRepository;
        this.findingRepository = findingRepository;
        this.sodWaiverRepository = sodWaiverRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public Audit submit(UUID auditId, String password, String signatureName) {
        Audit audit = requireStatus(auditId, AuditStatus.IN_PROGRESS);
        UUID actor = currentUserProvider.current().id();

        // AC-AUD-23-X: closure blocked while any scope-change disposition remains open.
        if (findingRepository.existsByAuditIdAndStatusAndScopeChangeDispositionIsNull(auditId, FindingStatus.SCOPE_CHANGED)) {
            throw new ConflictException("SCOPE_CHANGE_DISPOSITION_OPEN",
                    "One or more assets flagged \"Scope Changed During Audit\" still need a disposition before this audit can be submitted");
        }

        // AC-AUD-13-X: re-authentication failure leaves the audit editable/unsubmitted - nothing below this has mutated yet.
        AppUser user = appUserRepository.findById(actor).orElseThrow(() -> NotFoundException.of("AppUser", actor));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ValidationFailedException.singleField("password", "Re-authentication failed - incorrect password");
        }

        UUID approverId = resolveApprover(audit, actor);

        // AC-AUD-09-H: unverified expected assets become Missing exactly here, at submission.
        classifyMissing(audit);

        audit.setEffectiveApproverId(approverId);
        audit.setSubmittedBy(actor);
        audit.setSubmittedAt(Instant.now());
        audit.setSignatureName(signatureName != null && !signatureName.isBlank() ? signatureName : user.getDisplayName());
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        audit.setUpdatedBy(actor);
        return auditRepository.saveAndFlush(audit);
    }

    /** US-AUD-22: submitter can never be their own approver - blocked, unless an active waiver reroutes to its signer instead. */
    private UUID resolveApprover(Audit audit, UUID actor) {
        if (!audit.getNominalApproverId().equals(actor)) {
            return audit.getNominalApproverId();
        }
        List<SodWaiver> activeWaivers = sodWaiverRepository.findActiveByScopeOrderByCreatedAtDesc(SOD_SCOPE);
        if (activeWaivers.isEmpty()) {
            throw ValidationFailedException.singleField("nominalApproverId",
                    "You are the nominal approver for this audit and cannot submit it yourself - "
                            + "route to an alternate approver, or record an active '" + SOD_SCOPE + "' SoD waiver first");
        }
        return activeWaivers.get(0).getSignedOffBy().getId();
    }

    private void classifyMissing(Audit audit) {
        List<AuditExpectedAsset> expected = expectedAssetRepository.findByAuditIdWithAsset(audit.getId());
        for (AuditExpectedAsset row : expected) {
            if (findingRepository.findByAuditIdAndAssetId(audit.getId(), row.getAsset().getId()).isPresent()) {
                continue;
            }
            AuditFinding finding = new AuditFinding();
            finding.setAudit(audit);
            finding.setAsset(row.getAsset());
            finding.setStatus(FindingStatus.MISSING);
            finding.setVerifiedAt(Instant.now());
            findingRepository.save(finding);
        }
    }

    @Transactional
    public Audit approve(UUID auditId) {
        Audit audit = requireStatus(auditId, AuditStatus.PENDING_APPROVAL);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(audit, actor);

        audit.setStatus(AuditStatus.CLOSED);
        audit.setApprovedBy(actor);
        audit.setApprovedAt(Instant.now());
        audit.setUpdatedBy(actor);
        return auditRepository.saveAndFlush(audit);
    }

    @Transactional
    public Audit reject(UUID auditId, String reason) {
        Audit audit = requireStatus(auditId, AuditStatus.PENDING_APPROVAL);
        UUID actor = currentUserProvider.current().id();
        requireIsRoutedApprover(audit, actor);

        audit.setStatus(AuditStatus.IN_PROGRESS);
        audit.setSubmittedBy(null);
        audit.setSubmittedAt(null);
        audit.setSignatureName(null);
        audit.setEffectiveApproverId(null);
        audit.setLastRejectionReason(reason);
        audit.setUpdatedBy(actor);
        // Undo this submission's own Missing classification (never a real scan - see
        // AuditFindingRepository's Javadoc) so the reopened audit's auditor can actually
        // rescan those assets; a real VERIFIED/OUT_OF_SCOPE finding is never touched here.
        findingRepository.deleteAll(findingRepository.findByAuditIdAndStatusAndVerifiedByUserIdIsNull(auditId, FindingStatus.MISSING));
        return auditRepository.saveAndFlush(audit);
    }

    private void requireIsRoutedApprover(Audit audit, UUID actor) {
        UUID approver = audit.getEffectiveApproverId() != null ? audit.getEffectiveApproverId() : audit.getNominalApproverId();
        if (!approver.equals(actor)) {
            throw new AccessDeniedException("Only this audit's routed approver may act on it");
        }
    }

    private Audit requireStatus(UUID auditId, AuditStatus expected) {
        Audit audit = auditRepository.findByIdWithAssociations(auditId).orElseThrow(() -> NotFoundException.of("Audit", auditId));
        if (audit.getStatus() != expected) {
            throw new ConflictException("AUDIT_WRONG_STATUS",
                    "Audit must be " + expected + " for this action; current status is " + audit.getStatus());
        }
        return audit;
    }
}
