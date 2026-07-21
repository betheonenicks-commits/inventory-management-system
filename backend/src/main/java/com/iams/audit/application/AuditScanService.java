package com.iams.audit.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditAssignmentRepository;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-05/06/07/10/12: recording scans against an in-progress audit, one
 * at a time or as a batch. Continuous scan mode (US-AUD-06) is a frontend
 * interaction pattern with no distinct backend need - both endpoints here are
 * stateless per call, so a frontend can call recordScan repeatedly with no
 * server-side "mode" to track.
 */
@Service
public class AuditScanService {

    private static final int MAX_REMARKS_LENGTH = 1000;

    private final AuditRepository auditRepository;
    private final AuditFindingRepository findingRepository;
    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditAssignmentRepository assignmentRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public AuditScanService(AuditRepository auditRepository, AuditFindingRepository findingRepository,
                             AuditExpectedAssetRepository expectedAssetRepository,
                             AuditAssignmentRepository assignmentRepository, AssetRepository assetRepository,
                             CurrentUserProvider currentUserProvider, AuditService auditService) {
        this.auditRepository = auditRepository;
        this.findingRepository = findingRepository;
        this.expectedAssetRepository = expectedAssetRepository;
        this.assignmentRepository = assignmentRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @Transactional
    public AuditFinding recordScan(UUID auditId, AuditScanCommand command) {
        Audit audit = requireInProgress(auditId);
        requireActiveAuditorIfAnyAssigned(audit);
        Asset asset = assetRepository.findById(command.assetId())
                .orElseThrow(() -> NotFoundException.of("Asset", command.assetId()));
        if (findingRepository.findByAuditIdAndAssetId(auditId, command.assetId()).isPresent()) {
            throw new ConflictException("FINDING_ALREADY_RECORDED",
                    "Asset " + asset.getAssetNumber() + " has already been scanned in this audit");
        }
        return createFinding(audit, asset, command);
    }

    /** US-AUD-07: a batch's results reported together - resolved/duplicate/unrecognized, not one scan at a time. */
    @Transactional
    public BatchScanResult recordBatchScan(UUID auditId, List<AuditScanCommand> commands) {
        Audit audit = requireInProgress(auditId);
        requireActiveAuditorIfAnyAssigned(audit);

        List<AuditFinding> created = new ArrayList<>();
        List<UUID> duplicateAssetIds = new ArrayList<>();
        List<UUID> unrecognizedAssetIds = new ArrayList<>();
        for (AuditScanCommand command : commands) {
            Optional<Asset> asset = assetRepository.findById(command.assetId());
            if (asset.isEmpty()) {
                unrecognizedAssetIds.add(command.assetId());
                continue;
            }
            if (findingRepository.findByAuditIdAndAssetId(auditId, command.assetId()).isPresent()) {
                duplicateAssetIds.add(command.assetId());
                continue;
            }
            created.add(createFinding(audit, asset.get(), command));
        }
        return new BatchScanResult(created, duplicateAssetIds, unrecognizedAssetIds);
    }

    private AuditFinding createFinding(Audit audit, Asset asset, AuditScanCommand command) {
        if (command.remarks() != null && command.remarks().length() > MAX_REMARKS_LENGTH) {
            // AC-AUD-12-X: rejected with the limit stated.
            throw ValidationFailedException.singleField("remarks",
                    "Remarks must be at most " + MAX_REMARKS_LENGTH + " characters");
        }
        boolean inScope = expectedAssetRepository.existsByAuditIdAndAssetId(audit.getId(), asset.getId());
        CurrentUser current = currentUserProvider.current();

        AuditFinding finding = new AuditFinding();
        finding.setAudit(audit);
        finding.setAsset(asset);
        // AC-AUD-05-X: an asset outside this audit's expected set is flagged OUT_OF_SCOPE, not silently accepted as verified.
        finding.setStatus(inScope ? FindingStatus.VERIFIED : FindingStatus.OUT_OF_SCOPE);
        finding.setCondition(command.condition());
        finding.setRemarks(command.remarks());
        finding.setVerifiedByUserId(current.id());
        finding.setVerifiedByUsername(current.username());
        finding.setDeviceId(command.deviceId());
        return findingRepository.save(finding);
    }

    private Audit requireInProgress(UUID auditId) {
        Audit audit = auditRepository.findByIdWithAssociations(auditId)
                .orElseThrow(() -> NotFoundException.of("Audit", auditId));
        auditService.requireInScope(audit); // US-AUD-05/07: scanning is org-scope-enforced, not just visible
        if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
            throw new ConflictException("AUDIT_NOT_IN_PROGRESS",
                    "Audit is not accepting scans in its current status: " + audit.getStatus());
        }
        return audit;
    }

    /** AC-AUD-02-X: once any assignment exists, only its active assignees may scan - an ended assignment is refused. */
    private void requireActiveAuditorIfAnyAssigned(Audit audit) {
        List<AuditAssignment> assignments = assignmentRepository.findByAuditIdOrderByCreatedAtAsc(audit.getId());
        if (assignments.isEmpty()) {
            return;
        }
        UUID actor = currentUserProvider.current().id();
        boolean isActiveAssignee = assignments.stream()
                .anyMatch(a -> a.getAuditorUserId().equals(actor) && a.isActive());
        if (!isActiveAssignee) {
            throw new AccessDeniedException("You are not an active auditor on this audit");
        }
    }

    public record BatchScanResult(List<AuditFinding> created, List<UUID> duplicateAssetIds, List<UUID> unrecognizedAssetIds) {
    }
}
