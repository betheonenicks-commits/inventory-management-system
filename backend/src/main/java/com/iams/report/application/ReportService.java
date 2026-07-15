package com.iams.report.application;

import com.iams.asset.application.DepreciationResult;
import com.iams.asset.application.DepreciationService;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.application.AuditReportService;
import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.dashboard.application.DashboardService;
import com.iams.maintenance.domain.MaintenanceEvent;
import com.iams.maintenance.domain.MaintenanceEventRepository;
import com.iams.maintenance.domain.RepairEvent;
import com.iams.maintenance.domain.RepairEventRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.procurement.domain.PurchaseOrder;
import com.iams.procurement.domain.PurchaseOrderLine;
import com.iams.sec.domain.SecurityEventLog;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.application.OrgScopeGuard;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EPIC-RPT's first cluster (US-RPT-01/02/03/05/07/14): the list reports whose
 * underlying queries prior epics already built and click-tested. Every report
 * is org-scoped for the caller (the same OrgScopeGuard rules as the source
 * modules), and every method returns the uniform {@link TabularReport} shape
 * the CSV exporter and the generic frontend renderer both consume.
 * <p>
 * Report rows are built inside the read-only transaction, so LAZY
 * associations resolve while the session is open - the report is plain
 * strings by the time it leaves this class.
 */
@Service
public class ReportService {

    private static final int CSV_PAGE_SIZE = 500;
    private static final int MAX_LOOKAHEAD_DAYS = 365;

    /** Same damage set AuditReportService uses for exceptions - a "Damaged" loss row means exactly what an audit exception means. */
    private static final List<AssetCondition> DAMAGE_CONDITIONS =
            List.of(AssetCondition.MINOR_DAMAGE, AssetCondition.MAJOR_DAMAGE, AssetCondition.UNUSABLE);

    private final AssetRepository assetRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final PersonRepository personRepository;
    private final SecurityEventLogRepository securityEventLogRepository;
    private final ReportQueries reportQueries;
    private final DashboardService dashboardService;
    private final OrgScopeGuard scopeGuard;
    private final AuditRepository auditRepository;
    private final AuditReportService auditReportService;
    private final DepreciationService depreciationService;
    private final RepairEventRepository repairEventRepository;
    private final MaintenanceEventRepository maintenanceEventRepository;

    public ReportService(AssetRepository assetRepository, OrgNodeRepository orgNodeRepository,
                          PersonRepository personRepository, SecurityEventLogRepository securityEventLogRepository,
                          ReportQueries reportQueries, DashboardService dashboardService, OrgScopeGuard scopeGuard,
                          AuditRepository auditRepository, AuditReportService auditReportService,
                          DepreciationService depreciationService, RepairEventRepository repairEventRepository,
                          MaintenanceEventRepository maintenanceEventRepository) {
        this.assetRepository = assetRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.personRepository = personRepository;
        this.securityEventLogRepository = securityEventLogRepository;
        this.reportQueries = reportQueries;
        this.dashboardService = dashboardService;
        this.scopeGuard = scopeGuard;
        this.auditRepository = auditRepository;
        this.auditReportService = auditReportService;
        this.depreciationService = depreciationService;
        this.repairEventRepository = repairEventRepository;
        this.maintenanceEventRepository = maintenanceEventRepository;
    }

    /**
     * US-RPT-01/02: the asset register, optionally narrowed to an org node
     * ("department, room, or building" - all org-hierarchy nodes; a
     * Department-dimension report stays blocked on US-ORG-03's known
     * Department-not-wired-to-Asset gap). A requested node outside the
     * caller's own scope is refused, not silently intersected - the caller
     * asked for something they may not see, and pretending the answer is
     * "empty" would misreport reality.
     */
    @Transactional(readOnly = true)
    public TabularReport assetRegister(UUID orgNodeId, UUID categoryId, UUID statusId) {
        String prefix = scopeGuard.currentScopePathPrefix();
        String title = "Asset Register";
        if (orgNodeId != null) {
            scopeGuard.requireWithinScope(orgNodeId, "org node", orgNodeId);
            OrgNode node = orgNodeRepository.findById(orgNodeId)
                    .orElseThrow(() -> NotFoundException.of("OrgNode", orgNodeId));
            prefix = node.getPath();
            title = "Asset Register - " + node.getName();
        }

        List<List<String>> rows = new ArrayList<>();
        Pageable page = PageRequest.of(0, CSV_PAGE_SIZE, Sort.by("assetNumber"));
        while (true) {
            var slice = assetRepository.search(categoryId, statusId, null, prefix, page);
            for (Asset asset : slice.getContent()) {
                rows.add(List.of(
                        asset.getAssetNumber(),
                        asset.getName(),
                        asset.getCategory().getName(),
                        asset.getStatus().getLabel(),
                        asset.getOrgNode().getName(),
                        nullable(asset.getSerialNumber()),
                        nullable(asset.getManufacturer()),
                        nullable(asset.getPurchaseDate()),
                        nullable(asset.getPurchaseCost()),
                        nullable(asset.getWarrantyEndDate())));
            }
            if (!slice.hasNext()) {
                break;
            }
            page = slice.nextPageable();
        }
        return report("asset-register", title,
                List.of("Asset Number", "Name", "Category", "Status", "Location", "Serial Number", "Manufacturer",
                        "Purchase Date", "Purchase Cost", "Warranty End"), rows);
    }

    /** US-RPT-03: everything currently assigned to one person, with the assignment date from the history trail. */
    @Transactional(readOnly = true)
    public TabularReport employeeAssets(UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> NotFoundException.of("Person", personId));
        List<Asset> assets = scopeGuard.filterToScope(assetRepository.findByAssignedToPersonId(personId),
                a -> a.getOrgNode().getId());

        // Latest ASSIGNMENT_CHANGE per asset = when the current holder got it.
        Map<UUID, Instant> assignedAt = new HashMap<>();
        for (AssetHistoryEvent event : reportQueries.assignmentEvents(assets.stream().map(Asset::getId).toList())) {
            assignedAt.putIfAbsent(event.getAsset().getId(), event.getCreatedAt());
        }

        List<List<String>> rows = assets.stream()
                .map(a -> List.of(
                        a.getAssetNumber(),
                        a.getName(),
                        a.getCategory().getName(),
                        a.getStatus().getLabel(),
                        a.getOrgNode().getName(),
                        nullable(assignedAt.get(a.getId()))))
                .toList();
        return report("employee-assets", "Assigned Assets - " + person.getFullName(),
                List.of("Asset Number", "Name", "Category", "Status", "Location", "Assigned At"), rows);
    }

    /** US-RPT-05: warranty/AMC/insurance expiry plus maintenance due - reuses the dashboard's merged, scoped query (US-DSH-03) with a report-sized default window. */
    @Transactional(readOnly = true)
    public TabularReport expiry(int withinDays) {
        List<List<String>> rows = dashboardService.expirations(withinDays).stream()
                .map(e -> List.of(e.kind().name(), e.assetName(), nullable(e.dueDate()), nullable(e.detail())))
                .toList();
        return report("expiry", "Expiring Coverage & Maintenance Due (next " + withinDays + " days)",
                List.of("Kind", "Asset", "Due Date", "Detail"), rows);
    }

    /** US-RPT-07: relocation activity in a date range, chronological. */
    @Transactional(readOnly = true)
    public TabularReport assetMovements(LocalDate from, LocalDate to) {
        requireRange(from, to);
        List<List<String>> rows = reportQueries.movements(
                        from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                        to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                        scopeGuard.currentScopePathPrefix()).stream()
                .map(e -> List.of(
                        e.getAsset().getAssetNumber(),
                        e.getAsset().getName(),
                        nullable(e.getOldValue()),
                        nullable(e.getNewValue()),
                        String.valueOf(e.getCreatedBy()),
                        e.getCreatedAt().toString()))
                .toList();
        return report("asset-movements", "Asset Movements " + from + " to " + to,
                List.of("Asset Number", "Asset", "From", "To", "Actor", "Moved At"), rows);
    }

    /**
     * US-RPT-14: the Security & Access Log as a formal, exportable report.
     * Gated security:read at the controller (not reports:read) - the AC
     * explicitly refuses a Viewer, consistent with US-SEC-11's live view.
     */
    @Transactional(readOnly = true)
    public TabularReport securityEvents(UUID actorUserId, SecurityEventType eventType, Instant from, Instant to) {
        List<List<String>> rows = securityEventLogRepository
                .search(actorUserId, eventType, from, to, PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(e -> List.of(
                        e.getCreatedAt().toString(),
                        e.getEventType().name(),
                        nullable(e.getActorUserId()),
                        nullable(e.getUsernameAttempted()),
                        nullable(e.getIpAddress()),
                        nullable(e.getDetail())))
                .toList();
        return report("security-events", "Security & Access Log",
                List.of("Timestamp", "Event Type", "Actor User Id", "Username Attempted", "IP Address", "Detail"), rows);
    }

    /**
     * US-RPT-04: every Missing or damaged finding across every audit, with its
     * source audit named on the row. Date range is optional (the AC's filter
     * case), inclusive of the end date when given.
     */
    @Transactional(readOnly = true)
    public TabularReport loss(LocalDate from, LocalDate to) {
        requireOptionalRange(from, to);
        List<List<String>> rows = reportQueries.lossFindings(
                        from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                        to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                        DAMAGE_CONDITIONS, scopeGuard.currentScopePathPrefix()).stream()
                .map(f -> List.of(
                        f.getAsset().getAssetNumber(),
                        f.getAsset().getName(),
                        f.getStatus().name(),
                        f.getCondition() != null ? f.getCondition().name() : "",
                        f.getAudit().getName(),
                        nullable(f.getRemarks()),
                        f.getVerifiedAt().toString()))
                .toList();
        return report("loss", "Missing / Lost / Damaged Assets",
                List.of("Asset Number", "Asset", "Finding", "Condition", "Source Audit", "Remarks", "Recorded At"), rows);
    }

    /**
     * US-RPT-06: item-level PO detail grouped by vendor with a subtotal row
     * per vendor and a grand total - "totals and item-level detail break down
     * by vendor" in one table. Vendor identity prefers the real Vendor link
     * (US-INV-07's FK) and falls back to the order's free-text vendor name,
     * which predates the Vendor entity.
     */
    @Transactional(readOnly = true)
    public TabularReport vendorPurchases(LocalDate from, LocalDate to) {
        requireOptionalRange(from, to);
        List<PurchaseOrderLine> lines = reportQueries.purchaseOrderLines(
                from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null);

        Map<String, List<PurchaseOrderLine>> byVendor = new LinkedHashMap<>();
        for (PurchaseOrderLine line : lines) {
            byVendor.computeIfAbsent(vendorLabel(line.getPurchaseOrder()), k -> new ArrayList<>()).add(line);
        }

        List<List<String>> rows = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        long grandOrdered = 0;
        for (Map.Entry<String, List<PurchaseOrderLine>> entry : byVendor.entrySet()) {
            BigDecimal vendorTotal = BigDecimal.ZERO;
            long vendorOrdered = 0;
            for (PurchaseOrderLine line : entry.getValue()) {
                BigDecimal lineTotal = line.getUnitCost().multiply(BigDecimal.valueOf(line.getQuantityOrdered()));
                vendorTotal = vendorTotal.add(lineTotal);
                vendorOrdered += line.getQuantityOrdered();
                PurchaseOrder order = line.getPurchaseOrder();
                rows.add(List.of(entry.getKey(), order.getPoNumber(), order.getStatus().name(),
                        line.getDescription(), String.valueOf(line.getQuantityOrdered()),
                        String.valueOf(line.getQuantityReceived()), String.valueOf(line.getQuantityReturned()),
                        line.getUnitCost().toPlainString(), lineTotal.toPlainString()));
            }
            rows.add(List.of(entry.getKey() + " — subtotal", "", "", entry.getValue().size() + " lines",
                    String.valueOf(vendorOrdered), "", "", "", vendorTotal.toPlainString()));
            grandTotal = grandTotal.add(vendorTotal);
            grandOrdered += vendorOrdered;
        }
        if (!byVendor.isEmpty()) {
            rows.add(List.of("TOTAL", "", "", lines.size() + " lines", String.valueOf(grandOrdered), "", "", "",
                    grandTotal.toPlainString()));
        }
        return report("vendor-purchases", "Purchase & Vendor Report",
                List.of("Vendor", "PO Number", "PO Status", "Item", "Qty Ordered", "Qty Received", "Qty Returned",
                        "Unit Cost", "Line Total"), rows);
    }

    /**
     * US-RPT-08: audit health across a period. Completion rate = closed/total.
     * Exception rate = closed audits with at least one exception, over closed.
     * On-time rate is computed only over audits that have both a scheduled
     * date (optional since V39) and a submission - an audit with no scheduled
     * date has no deadline to be late against, and pretending otherwise would
     * fabricate a metric; the denominator is stated on the summary row. Open
     * audits are listed with their live status, never counted as complete.
     */
    @Transactional(readOnly = true)
    public TabularReport auditCompliance(LocalDate from, LocalDate to) {
        requireOptionalRange(from, to);
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.MAX;
        String scope = scopeGuard.currentScopePathPrefix();
        List<Audit> audits = auditRepository.findAllWithAssociationsOrderByCreatedAtDesc().stream()
                .filter(a -> !a.getCreatedAt().isBefore(fromInstant) && a.getCreatedAt().isBefore(toInstant))
                // Same scope rule as AuditService.list(): org-scoped audits filter by
                // subtree; category/asset-list-scoped audits pass through (documented US-AUD-03 gap).
                .filter(a -> scope == null || a.getScopeOrgNode() == null
                        || a.getScopeOrgNode().getPath().startsWith(scope))
                .toList();

        long closed = audits.stream().filter(a -> a.getStatus() == AuditStatus.CLOSED).count();
        long closedWithExceptions = audits.stream()
                .filter(a -> a.getStatus() == AuditStatus.CLOSED)
                .filter(a -> !auditReportService.exceptions(a.getId()).isEmpty())
                .count();
        List<Audit> withDeadline = audits.stream()
                .filter(a -> a.getScheduledDate() != null && a.getSubmittedAt() != null)
                .toList();
        long onTime = withDeadline.stream()
                .filter(a -> a.getSubmittedAt().isBefore(a.getScheduledDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .count();

        List<List<String>> rows = new ArrayList<>();
        rows.add(summaryRow("Completion rate", rate(closed, audits.size()) + " (" + closed + "/" + audits.size() + " closed)"));
        rows.add(summaryRow("Exception rate", rate(closedWithExceptions, closed) + " (" + closedWithExceptions + "/" + closed + " closed audits with exceptions)"));
        rows.add(summaryRow("On-time rate", rate(onTime, withDeadline.size()) + " (" + onTime + "/" + withDeadline.size() + " audits with a scheduled date and a submission)"));
        for (Audit audit : audits) {
            rows.add(List.of(audit.getName(), audit.getStatus().name(),
                    audit.getCreatedAt().toString(),
                    nullable(audit.getScheduledDate()),
                    nullable(audit.getSubmittedAt()),
                    audit.getStatus() == AuditStatus.CLOSED
                            ? (auditReportService.exceptions(audit.getId()).isEmpty() ? "none" : "yes")
                            : "n/a (open)",
                    onTimeCell(audit)));
        }
        return report("audit-compliance", "Audit Compliance Summary",
                List.of("Audit", "Status", "Created", "Scheduled Date", "Submitted At", "Exceptions", "On Time"), rows);
    }

    /**
     * US-RPT-09: net book value per in-scope asset as of the report date,
     * computed from each asset's own stored parameters (FR-AST-16). An asset
     * with nothing configured is explicitly "NOT_DEPRECIATED", never a
     * misleading zero; an asset whose configured method the engine can't
     * compute yet degrades to its own flagged row rather than failing the
     * whole report (the same per-row-degradation principle as US-RPT-11's
     * batch AC).
     */
    @Transactional(readOnly = true)
    public TabularReport depreciation(LocalDate asOf) {
        LocalDate effectiveAsOf = asOf != null ? asOf : LocalDate.now();
        String prefix = scopeGuard.currentScopePathPrefix();
        List<List<String>> rows = new ArrayList<>();
        Pageable page = PageRequest.of(0, CSV_PAGE_SIZE, Sort.by("assetNumber"));
        while (true) {
            var slice = assetRepository.search(null, null, null, prefix, page);
            for (Asset asset : slice.getContent()) {
                rows.add(depreciationRow(asset, effectiveAsOf));
            }
            if (!slice.hasNext()) {
                break;
            }
            page = slice.nextPageable();
        }
        return report("depreciation", "Depreciation & Net Book Value as of " + effectiveAsOf,
                List.of("Asset Number", "Asset", "Category", "Status", "Purchase Cost", "Method",
                        "Accumulated Depreciation", "Net Book Value"), rows);
    }

    /**
     * US-RPT-10: repairs plus preventive/corrective maintenance in one
     * timeline, newest first, optionally narrowed to one asset. Downtime is
     * only ever stated for a closed repair (opened -> actual return); an open
     * repair or a point-in-time maintenance completion has no downtime to
     * report, and the cell stays empty rather than guessing.
     */
    @Transactional(readOnly = true)
    public TabularReport maintenanceHistory(UUID assetId) {
        if (assetId != null && !assetRepository.existsById(assetId)) {
            throw NotFoundException.of("Asset", assetId);
        }
        List<RepairEvent> repairs = scopeGuard.filterToScope(
                assetId != null ? repairEventRepository.findByAssetIdWithAssetOrderByCreatedAtDesc(assetId)
                        : repairEventRepository.findAllWithAssetOrderByCreatedAtDesc(),
                r -> r.getAsset().getOrgNode().getId());
        List<MaintenanceEvent> events = scopeGuard.filterToScope(
                assetId != null ? maintenanceEventRepository.findByAssetIdWithAssociationsOrderByPerformedAtDesc(assetId)
                        : maintenanceEventRepository.findAllWithAssociationsOrderByPerformedAtDesc(),
                e -> e.getAsset().getOrgNode().getId());

        record Entry(Instant at, List<String> cells) {
        }
        List<Entry> entries = new ArrayList<>();
        for (RepairEvent r : repairs) {
            entries.add(new Entry(r.getCreatedAt(), List.of(
                    r.getAsset().getAssetNumber(), r.getAsset().getName(), "REPAIR",
                    r.getCreatedAt().toString(),
                    r.getActualCost() != null ? r.getActualCost().toPlainString()
                            : (r.getEstimatedCost() != null ? r.getEstimatedCost().toPlainString() + " (est.)" : ""),
                    repairDowntime(r),
                    nullable(r.getReason()))));
        }
        for (MaintenanceEvent e : events) {
            entries.add(new Entry(e.getPerformedAt(), List.of(
                    e.getAsset().getAssetNumber(), e.getAsset().getName(), e.getMaintenanceType().name(),
                    e.getPerformedAt().toString(),
                    e.getCost() != null ? e.getCost().toPlainString() : "",
                    "",
                    nullable(e.getNotes()))));
        }
        entries.sort(Comparator.comparing(Entry::at).reversed());
        return report("maintenance-history", "Maintenance History",
                List.of("Asset Number", "Asset", "Type", "Date", "Cost", "Downtime (days)", "Notes"),
                entries.stream().map(Entry::cells).toList());
    }

    private List<String> depreciationRow(Asset asset, LocalDate asOf) {
        String costCell = nullable(asset.getPurchaseCost());
        try {
            DepreciationResult result = depreciationService.compute(asset.getId(), asOf);
            if (result.status() == DepreciationResult.Status.NOT_DEPRECIATED) {
                return List.of(asset.getAssetNumber(), asset.getName(), asset.getCategory().getName(),
                        "NOT_DEPRECIATED", costCell, "", "", "");
            }
            return List.of(asset.getAssetNumber(), asset.getName(), asset.getCategory().getName(),
                    "COMPUTED", costCell, result.method().name(),
                    result.accumulatedDepreciation().toPlainString(), result.netBookValue().toPlainString());
        } catch (ValidationFailedException ex) {
            // e.g. a method the engine doesn't support yet - flag the row, keep the report.
            return List.of(asset.getAssetNumber(), asset.getName(), asset.getCategory().getName(),
                    "UNSUPPORTED", costCell, "", "", "");
        }
    }

    private static String repairDowntime(RepairEvent repair) {
        if (repair.getActualReturnDate() == null) {
            return "";
        }
        long days = Duration.between(repair.getCreatedAt(),
                repair.getActualReturnDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()).toDays();
        return String.valueOf(Math.max(days, 0));
    }

    private static String vendorLabel(PurchaseOrder order) {
        return order.getVendor() != null ? order.getVendor().getName() : order.getVendorName();
    }

    private static String onTimeCell(Audit audit) {
        if (audit.getScheduledDate() == null || audit.getSubmittedAt() == null) {
            return "n/a";
        }
        boolean onTime = audit.getSubmittedAt()
                .isBefore(audit.getScheduledDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        return onTime ? "yes" : "late";
    }

    private static List<String> summaryRow(String metric, String value) {
        return List.of("— " + metric, value, "", "", "", "", "");
    }

    private static String rate(long numerator, long denominator) {
        return denominator == 0 ? "n/a" : Math.round(numerator * 100.0 / denominator) + "%";
    }

    private static void requireOptionalRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw ValidationFailedException.singleField("to", "Must not be before from");
        }
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ValidationFailedException.singleField("from", "Both from and to dates are required");
        }
        if (to.isBefore(from)) {
            throw ValidationFailedException.singleField("to", "Must not be before from");
        }
    }

    private static TabularReport report(String key, String title, List<String> columns, List<List<String>> rows) {
        return new TabularReport(key, title, Instant.now(), columns, rows);
    }

    private static String nullable(Object value) {
        return Objects.toString(value, "");
    }
}
