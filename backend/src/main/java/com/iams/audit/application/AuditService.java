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
import com.iams.audit.domain.AuditSubScopeCount;
import com.iams.audit.domain.AuditSubScopeStatusCount;
import com.iams.audit.domain.FindingStatus;
import com.iams.audit.domain.SampleSizeCalculator;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    /** US-AUD-20: margin of error used when sampling is requested without an explicit one. */
    private static final double DEFAULT_MARGIN_OF_ERROR = 5.0;

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
        List<Asset> population = resolveExpectedAssets(command, scopeOrgNode);
        List<Asset> expected = population;
        if (command.samplingRequested()) {
            // US-AUD-20: freeze a random statistical sample instead of the full population.
            double margin = command.samplingMarginOfError() != null ? command.samplingMarginOfError() : DEFAULT_MARGIN_OF_ERROR;
            long sampleSize = SampleSizeCalculator.sampleSize(population.size(), command.samplingConfidenceLevel(), margin);
            expected = randomSample(population, (int) sampleSize);
            audit.setSamplingConfidenceLevel(command.samplingConfidenceLevel());
            audit.setSamplingMarginOfError(BigDecimal.valueOf(margin));
            audit.setSamplingPopulationSize(population.size());
        }
        for (Asset asset : expected) {
            AuditExpectedAsset row = new AuditExpectedAsset();
            row.setAudit(audit);
            row.setAsset(asset);
            expectedAssetRepository.save(row);
        }
        return get(audit.getId());
    }

    /** A random subset of {@code size} assets (a fresh shuffle each time); size is already clamped to [0, population]. */
    private List<Asset> randomSample(List<Asset> population, int size) {
        if (size >= population.size()) {
            return population;
        }
        List<Asset> shuffled = new ArrayList<>(population);
        Collections.shuffle(shuffled, new Random());
        return shuffled.subList(0, size);
    }

    /**
     * US-AUD-20: preview how many assets a statistical sample of the given scope would
     * cover, before any audit is created or scanning begins. Pure calculation over the
     * scope's live population count - the exact same {@link SampleSizeCalculator} the
     * frozen sample uses at creation, so the preview can never disagree with reality.
     */
    @Transactional(readOnly = true)
    public SampleSizePreview sampleSizePreview(UUID scopeOrgNodeId, UUID scopeCategoryId, List<UUID> assetIds,
                                               int confidenceLevel, Double marginOfError) {
        boolean hasAssetList = assetIds != null && !assetIds.isEmpty();
        if (scopeOrgNodeId == null && scopeCategoryId == null && !hasAssetList) {
            throw ValidationFailedException.singleField("scope",
                    "At least one scoping criterion (org node, category, or asset list) is required");
        }
        double margin = marginOfError != null ? marginOfError : DEFAULT_MARGIN_OF_ERROR;
        long population;
        if (hasAssetList) {
            population = assetIds.size();
        } else {
            String pathPrefix = scopeOrgNodeId != null
                    ? orgNodeRepository.findById(scopeOrgNodeId)
                            .orElseThrow(() -> NotFoundException.of("OrgNode", scopeOrgNodeId)).getPath()
                    : null;
            // A count-only page: getTotalElements() is the scope's asset count, which is
            // exactly what creation freezes. (An earlier comment here claimed search's count
            // and content disagreed for scopes with child assets - that was a false alarm
            // from a test-harness parsing bug, since retracted; see DEVELOPMENT_LOG.md.)
            population = assetRepository.search(scopeCategoryId, null, null, null, pathPrefix, null, null,
                    PageRequest.of(0, 1)).getTotalElements();
        }
        long sampleSize = SampleSizeCalculator.sampleSize(population, confidenceLevel, margin);
        return new SampleSizePreview(population, confidenceLevel, margin, sampleSize);
    }

    /** US-AUD-20: a sample-size preview for a scope. */
    public record SampleSizePreview(long populationSize, int confidenceLevel, double marginOfError, long sampleSize) {
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
        requireAuditInScope(audit);
        return audit;
    }

    /**
     * US-USR-04 org-scope for audits, including the no-org-node case. An org-node-scoped
     * audit is visible within its node's subtree (unchanged). A category- or asset-list-
     * scoped audit has no org node of its own, so its footprint is the set of its
     * expected-asset locations: a scoped caller may see it iff at least one of those
     * assets falls within their scope - previously such audits bypassed scope entirely.
     */
    private void requireAuditInScope(Audit audit) {
        String scopePrefix = scopeGuard.currentScopePathPrefix();
        if (scopePrefix == null) {
            return; // unrestricted caller
        }
        if (audit.getScopeOrgNode() != null) {
            scopeGuard.requireWithinScope(audit.getScopeOrgNode().getId(), "audit", audit.getId());
        } else {
            scopeGuard.requireInScope(expectedAssetRepository.existsInScope(audit.getId(), scopePrefix), "audit", audit.getId());
        }
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
        // US-USR-04 / AUD-03: an org-node-scoped audit is filtered by its node's path;
        // a no-org-node (category/asset-list) audit is visible iff it has an expected
        // asset within the caller's scope - resolved with ONE batched query, not per row.
        List<UUID> noNodeIds = audits.stream()
                .filter(a -> a.getScopeOrgNode() == null)
                .map(Audit::getId)
                .toList();
        java.util.Set<UUID> visibleNoNode = noNodeIds.isEmpty()
                ? java.util.Set.of()
                : java.util.Set.copyOf(expectedAssetRepository.findAuditIdsInScope(noNodeIds, scopePrefix));
        return audits.stream()
                .filter(a -> a.getScopeOrgNode() != null
                        ? a.getScopeOrgNode().getPath().startsWith(scopePrefix)
                        : visibleNoNode.contains(a.getId()))
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
        get(auditId); // US-USR-04: 404 if missing, 403 if outside the caller's org scope
        return assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId);
    }

    @Transactional(readOnly = true)
    public AuditProgress progress(UUID auditId) {
        get(auditId); // US-USR-04: 404 if missing, 403 if outside the caller's org scope (previously unenforced here)
        long expected = expectedAssetRepository.countByAuditId(auditId);
        long verified = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED);
        long missing = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING);
        long outOfScope = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.OUT_OF_SCOPE);
        long scopeChanged = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.SCOPE_CHANGED);
        return new AuditProgress(expected, verified, missing, outOfScope, scopeChanged);
    }

    /**
     * US-AUD-03: the audit's progress as a flat total AND broken down by sub-scope
     * (the distinct org nodes/locations its expected-asset set spans), so a bulk
     * audit "across a wide scope" doesn't collapse to a single opaque percentage.
     * Kept separate from {@link #progress} on purpose: the dashboard calls
     * {@code progress} once per audit and never wants the per-location breakdown's
     * cost or payload - only the audit-detail view, which calls this, does.
     */
    @Transactional(readOnly = true)
    public AuditProgressDetail progressDetail(UUID auditId) {
        AuditProgress totals = progress(auditId); // 404s here if the audit doesn't exist
        return new AuditProgressDetail(totals, buildSubScopes(auditId));
    }

    /**
     * Assembles the per-location breakdown from two DB-side aggregates. The universe
     * of sub-scopes is the UNION of locations that have an expected asset and
     * locations that have a finding - so an OUT_OF_SCOPE find at a location holding
     * no expected asset still surfaces (expected 0, outOfScope >= 1), which keeps the
     * breakdown reconciling exactly with the flat totals for every status column.
     */
    private List<SubScopeProgress> buildSubScopes(UUID auditId) {
        Map<UUID, SubScopeAccumulator> byNode = new LinkedHashMap<>();
        for (AuditSubScopeCount row : expectedAssetRepository.countExpectedByOrgNode(auditId)) {
            byNode.computeIfAbsent(row.orgNodeId(),
                    k -> new SubScopeAccumulator(row.orgNodeId(), row.orgNodeName(), row.orgNodeCode()))
                    .expected = row.count();
        }
        for (AuditSubScopeStatusCount row : findingRepository.countFindingsByOrgNodeAndStatus(auditId)) {
            SubScopeAccumulator acc = byNode.computeIfAbsent(row.orgNodeId(),
                    k -> new SubScopeAccumulator(row.orgNodeId(), row.orgNodeName(), row.orgNodeCode()));
            switch (row.status()) {
                case VERIFIED -> acc.verified = row.count();
                case MISSING -> acc.missing = row.count();
                case OUT_OF_SCOPE -> acc.outOfScope = row.count();
                case SCOPE_CHANGED -> acc.scopeChanged = row.count();
            }
        }
        return byNode.values().stream()
                .sorted(Comparator.comparing((SubScopeAccumulator a) -> a.orgNodeName,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(a -> a.orgNodeId))
                .map(a -> new SubScopeProgress(a.orgNodeId, a.orgNodeName, a.orgNodeCode,
                        a.expected, a.verified, a.missing, a.outOfScope, a.scopeChanged))
                .toList();
    }

    /** Mutable per-location tally used only while assembling {@link #buildSubScopes}. */
    private static final class SubScopeAccumulator {
        private final UUID orgNodeId;
        private final String orgNodeName;
        private final String orgNodeCode;
        private long expected;
        private long verified;
        private long missing;
        private long outOfScope;
        private long scopeChanged;

        private SubScopeAccumulator(UUID orgNodeId, String orgNodeName, String orgNodeCode) {
            this.orgNodeId = orgNodeId;
            this.orgNodeName = orgNodeName;
            this.orgNodeCode = orgNodeCode;
        }
    }

    /** US-AUD-08: live expected-vs-verified progress. No offline-queue concept exists (US-AUD-19 is not built), so there is no "pending sync" count. */
    public record AuditProgress(long expectedCount, long verifiedCount, long missingCount, long outOfScopeCount, long scopeChangedCount) {
    }

    /** US-AUD-03: one org node (location) the audit spans, with its own progress counts. */
    public record SubScopeProgress(UUID orgNodeId, String orgNodeName, String orgNodeCode,
                                   long expectedCount, long verifiedCount, long missingCount,
                                   long outOfScopeCount, long scopeChangedCount) {
    }

    /** US-AUD-03: flat totals plus the per-sub-scope breakdown, for the audit-detail progress view. */
    public record AuditProgressDetail(AuditProgress totals, List<SubScopeProgress> subScopes) {
    }
}
