package com.iams.audit.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditAssignmentRepository;
import com.iams.audit.domain.AuditExpectedAsset;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-01/02/03/04/08: create an audit and freeze its expected-asset set,
 * assign/unassign auditors, list/read audits (org-scoped the same way
 * AssetQueryService is), and report live progress.
 */
@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditAssignmentRepository assignmentRepository;
    private final AuditFindingRepository findingRepository;
    private final AssetRepository assetRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public AuditService(AuditRepository auditRepository, AuditExpectedAssetRepository expectedAssetRepository,
                         AuditAssignmentRepository assignmentRepository, AuditFindingRepository findingRepository,
                         AssetRepository assetRepository, OrgNodeRepository orgNodeRepository,
                         AssetCategoryRepository categoryRepository, AppUserRepository appUserRepository,
                         CurrentUserProvider currentUserProvider, OrgScopeGuard scopeGuard) {
        this.auditRepository = auditRepository;
        this.expectedAssetRepository = expectedAssetRepository;
        this.assignmentRepository = assignmentRepository;
        this.findingRepository = findingRepository;
        this.assetRepository = assetRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.categoryRepository = categoryRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional
    public Audit create(AuditCreateCommand command) {
        boolean hasAssetList = command.assetIds() != null && !command.assetIds().isEmpty();
        if (command.scopeOrgNodeId() == null && command.scopeCategoryId() == null && !hasAssetList) {
            // AC-AUD-01-X: blocked until at least one scoping criterion is set.
            throw ValidationFailedException.singleField("scope",
                    "At least one scoping criterion (org node, category, or asset list) is required");
        }
        if (!appUserRepository.existsById(command.nominalApproverId())) {
            throw NotFoundException.of("AppUser", command.nominalApproverId());
        }

        OrgNode scopeOrgNode = command.scopeOrgNodeId() != null
                ? orgNodeRepository.findById(command.scopeOrgNodeId())
                        .orElseThrow(() -> NotFoundException.of("OrgNode", command.scopeOrgNodeId()))
                : null;
        AssetCategory scopeCategory = command.scopeCategoryId() != null
                ? categoryRepository.findById(command.scopeCategoryId())
                        .orElseThrow(() -> NotFoundException.of("AssetCategory", command.scopeCategoryId()))
                : null;

        UUID actor = currentUserProvider.current().id();
        Audit audit = new Audit();
        audit.setName(command.name());
        audit.setAuditType(command.auditType());
        audit.setScopeOrgNode(scopeOrgNode);
        audit.setScopeCategory(scopeCategory);
        audit.setStatus(AuditStatus.IN_PROGRESS);
        audit.setNominalApproverId(command.nominalApproverId());
        audit.setScheduledDate(command.scheduledDate());
        audit.setCreatedBy(actor);
        audit = auditRepository.save(audit);

        // US-AUD-04: resolved and frozen once, here, at creation - never re-run later.
        for (Asset asset : resolveExpectedAssets(command, scopeOrgNode)) {
            AuditExpectedAsset row = new AuditExpectedAsset();
            row.setAudit(audit);
            row.setAsset(asset);
            expectedAssetRepository.save(row);
        }
        return get(audit.getId());
    }

    private List<Asset> resolveExpectedAssets(AuditCreateCommand command, OrgNode scopeOrgNode) {
        if (command.assetIds() != null && !command.assetIds().isEmpty()) {
            List<Asset> assets = assetRepository.findAllById(command.assetIds());
            if (assets.size() != command.assetIds().size()) {
                throw ValidationFailedException.singleField("assetIds", "One or more asset IDs do not exist");
            }
            return assets;
        }
        String pathPrefix = scopeOrgNode != null ? scopeOrgNode.getPath() : null;
        // Pageable.unpaged() throws UnsupportedOperationException from AssetRepositoryImpl's
        // getOffset()/getPageSize() calls - a large explicit page stands in for "all matching".
        Page<Asset> page = assetRepository.search(command.scopeCategoryId(), null, null, null, pathPrefix,
                null, null, PageRequest.of(0, Integer.MAX_VALUE));
        return page.getContent();
    }

    @Transactional(readOnly = true)
    public Audit get(UUID id) {
        Audit audit = auditRepository.findByIdWithAssociations(id).orElseThrow(() -> NotFoundException.of("Audit", id));
        if (audit.getScopeOrgNode() != null) {
            scopeGuard.requireWithinScope(audit.getScopeOrgNode().getId(), "audit", id);
        }
        return audit;
    }

    @Transactional(readOnly = true)
    public List<Audit> list(AuditStatus status) {
        List<Audit> audits = status != null
                ? auditRepository.findByStatusWithAssociationsOrderByCreatedAtDesc(status)
                : auditRepository.findAllWithAssociationsOrderByCreatedAtDesc();
        String scopePrefix = scopeGuard.currentScopePathPrefix();
        if (scopePrefix == null) {
            return audits;
        }
        // US-AUD-03: category- or asset-list-scoped audits have no org node to filter by and
        // pass through unrestricted here - a documented gap, not an oversight (see DEVELOPMENT_LOG.md).
        return audits.stream()
                .filter(a -> a.getScopeOrgNode() == null || a.getScopeOrgNode().getPath().startsWith(scopePrefix))
                .toList();
    }

    @Transactional
    public AuditAssignment assignAuditor(UUID auditId, UUID auditorUserId, String subScope) {
        Audit audit = get(auditId);
        AppUser auditor = appUserRepository.findById(auditorUserId)
                .orElseThrow(() -> NotFoundException.of("AppUser", auditorUserId));

        AuditAssignment assignment = new AuditAssignment();
        assignment.setAudit(audit);
        assignment.setAuditorUserId(auditorUserId);
        assignment.setAuditorUsername(auditor.getUsername());
        assignment.setSubScope(subScope);
        assignment.setActive(true);
        assignment.setCreatedBy(currentUserProvider.current().id());
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public AuditAssignment unassignAuditor(UUID auditId, UUID assignmentId) {
        AuditAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> NotFoundException.of("AuditAssignment", assignmentId));
        if (!assignment.getAudit().getId().equals(auditId)) {
            throw NotFoundException.of("AuditAssignment", assignmentId);
        }
        // AC-AUD-02-X: ends the assignment but keeps the row - their prior contribution
        // to the audit's findings stays attributed to them, untouched.
        assignment.setActive(false);
        assignment.setUnassignedAt(Instant.now());
        assignment.setUpdatedBy(currentUserProvider.current().id());
        return assignmentRepository.saveAndFlush(assignment);
    }

    @Transactional(readOnly = true)
    public List<AuditAssignment> assignments(UUID auditId) {
        return assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId);
    }

    @Transactional(readOnly = true)
    public AuditProgress progress(UUID auditId) {
        if (!auditRepository.existsById(auditId)) {
            throw NotFoundException.of("Audit", auditId);
        }
        long expected = expectedAssetRepository.countByAuditId(auditId);
        long verified = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED);
        long missing = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING);
        long outOfScope = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.OUT_OF_SCOPE);
        long scopeChanged = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.SCOPE_CHANGED);
        return new AuditProgress(expected, verified, missing, outOfScope, scopeChanged);
    }

    /** US-AUD-08: live expected-vs-verified progress. No offline-queue concept exists (US-AUD-19 is not built), so there is no "pending sync" count. */
    public record AuditProgress(long expectedCount, long verifiedCount, long missingCount, long outOfScopeCount, long scopeChangedCount) {
    }
}
