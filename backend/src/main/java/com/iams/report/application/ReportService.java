package com.iams.report.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.dashboard.application.DashboardService;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.sec.domain.SecurityEventLog;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.application.OrgScopeGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final AssetRepository assetRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final PersonRepository personRepository;
    private final SecurityEventLogRepository securityEventLogRepository;
    private final ReportQueries reportQueries;
    private final DashboardService dashboardService;
    private final OrgScopeGuard scopeGuard;

    public ReportService(AssetRepository assetRepository, OrgNodeRepository orgNodeRepository,
                          PersonRepository personRepository, SecurityEventLogRepository securityEventLogRepository,
                          ReportQueries reportQueries, DashboardService dashboardService, OrgScopeGuard scopeGuard) {
        this.assetRepository = assetRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.personRepository = personRepository;
        this.securityEventLogRepository = securityEventLogRepository;
        this.reportQueries = reportQueries;
        this.dashboardService = dashboardService;
        this.scopeGuard = scopeGuard;
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
