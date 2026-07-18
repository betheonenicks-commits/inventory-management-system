package com.iams.dashboard.application;

import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.audit.application.AuditReportService;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.dashboard.application.DashboardQueries.ExpiringEntry;
import com.iams.dashboard.application.DashboardQueries.LabelCount;
import com.iams.inventory.application.InventoryStockService;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EPIC-DSH's widget aggregations (US-DSH-01/02/03/04/05), each org-scoped for
 * the calling user (US-DSH-07) by pushing OrgScopeGuard's path prefix into the
 * query rather than post-filtering.
 * <p>
 * Staleness policy (the "documented cache policy" US-DSH-01/02's ACs cite):
 * there is deliberately no cache - every widget reads live. The ACs bound
 * staleness at 5 minutes (asset summary) and 30 seconds (audit completion);
 * zero staleness satisfies both bounds without introducing cache
 * infrastructure nothing else in this codebase needs yet. If a cache is ever
 * added, those two ceilings are the contract it must stay inside.
 */
@Service
public class DashboardService {

    private static final int MAX_LOOKAHEAD_DAYS = 365;
    private static final int MAX_FEED_LIMIT = 100;
    private static final int RECENT_CLOSED_LIMIT = 5;

    private final DashboardQueries queries;
    private final OrgScopeGuard scopeGuard;
    private final AuditReportService auditReportService;
    private final InventoryStockService stockService;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final AuditRepository auditRepository;

    public DashboardService(DashboardQueries queries, OrgScopeGuard scopeGuard,
                             AuditReportService auditReportService, InventoryStockService stockService,
                             MaintenanceScheduleRepository maintenanceScheduleRepository,
                             AuditRepository auditRepository) {
        this.queries = queries;
        this.scopeGuard = scopeGuard;
        this.auditReportService = auditReportService;
        this.stockService = stockService;
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
        this.auditRepository = auditRepository;
    }

    /** US-DSH-01: totals plus per-category and per-status breakdowns, scoped. */
    @Transactional(readOnly = true)
    public AssetSummary assetSummary() {
        String scope = scopeGuard.currentScopePathPrefix();
        return new AssetSummary(queries.assetCount(scope), queries.assetCountByCategory(scope),
                queries.assetCountByStatus(scope));
    }

    /**
     * US-DSH-02 / US-AUD-17: per-audit completion for every active audit in
     * scope (with its exception count, and status - PENDING_APPROVAL being the
     * "pending approval" the AC wants visible), the average across those active
     * audits, plus a "recently closed" section so the dashboard shows in-progress
     * AND recent audits without opening each one. Reuses AuditReportService,
     * already scope-filtered via AuditService.list(). An audit with an empty
     * expected set counts as 100% - there is nothing left to verify. The average
     * is over ACTIVE audits only: a pile of 100%-complete closed audits must not
     * flatter the "how are the audits I still have to finish doing" number.
     */
    @Transactional(readOnly = true)
    public AuditCompletion auditCompletion() {
        List<AuditCompletionItem> items = auditReportService.dashboard().stream()
                .map(this::toCompletionItem)
                .toList();
        Integer average = items.isEmpty() ? null
                : (int) Math.round(items.stream().mapToInt(AuditCompletionItem::percentComplete).average().orElse(0));
        List<AuditCompletionItem> recentlyClosed = auditReportService.recentlyClosed(RECENT_CLOSED_LIMIT).stream()
                .map(this::toCompletionItem)
                .toList();
        return new AuditCompletion(items, average, recentlyClosed);
    }

    private AuditCompletionItem toCompletionItem(AuditReportService.AuditDashboardItem item) {
        return new AuditCompletionItem(item.auditId(), item.name(), item.status().name(),
                percentComplete(item.progress().expectedCount(), item.progress().verifiedCount()),
                item.exceptionCount());
    }

    /**
     * US-DSH-03: warranty ends, insurance expiries, and maintenance due inside
     * the lookahead window, merged and sorted by nearest date. Maintenance
     * includes already-overdue schedules (due date in the past) on purpose -
     * an overdue service must not vanish from the widget - while warranty and
     * insurance are window-only: an already-expired policy is a fact, not an
     * upcoming lapse to act on.
     */
    @Transactional(readOnly = true)
    public List<Expiration> expirations(int withinDays) {
        requireLookahead(withinDays);
        LocalDate today = LocalDate.now();
        LocalDate to = today.plusDays(withinDays);
        String scope = scopeGuard.currentScopePathPrefix();

        List<Expiration> merged = new ArrayList<>();
        for (ExpiringEntry e : queries.warrantyExpirations(today, to, scope)) {
            merged.add(new Expiration(ExpirationKind.WARRANTY, e.assetId(), e.assetName(), e.dueDate(), e.detail()));
        }
        for (ExpiringEntry e : queries.insuranceExpirations(today, to, scope)) {
            merged.add(new Expiration(ExpirationKind.INSURANCE, e.assetId(), e.assetName(), e.dueDate(), e.detail()));
        }
        scopeGuard.filterToScope(maintenanceScheduleRepository.findDueOnOrBefore(to),
                        s -> s.getAsset().getOrgNode().getId())
                .forEach(s -> merged.add(new Expiration(ExpirationKind.MAINTENANCE, s.getAsset().getId(),
                        s.getAsset().getName(), s.getNextDueDate(), s.getDescription())));
        merged.sort(Comparator.comparing(Expiration::dueDate));
        return merged;
    }

    /** US-DSH-04: delegates to the existing low-stock query (US-INV-04) unchanged. */
    @Transactional(readOnly = true)
    public List<InventoryStockService.LowStockItem> lowStock() {
        return stockService.lowStockItems();
    }

    /** US-DSH-05 (feed half): most recent asset-history events in scope, newest first. */
    @Transactional(readOnly = true)
    public List<AssetHistoryEvent> activityFeed(int limit) {
        if (limit < 1 || limit > MAX_FEED_LIMIT) {
            throw ValidationFailedException.singleField("limit", "Must be between 1 and " + MAX_FEED_LIMIT);
        }
        return queries.recentActivity(scopeGuard.currentScopePathPrefix(), limit);
    }

    /**
     * US-DSH-05 (calendar half): audits with a scheduled date inside the window,
     * scope-filtered the same way AuditService.list() is - an audit with no org
     * scope node (category- or asset-list-scoped) passes through, the same
     * documented US-AUD-03 gap that filter has always had.
     */
    @Transactional(readOnly = true)
    public List<Audit> auditCalendar(int withinDays) {
        requireLookahead(withinDays);
        LocalDate today = LocalDate.now();
        List<Audit> audits = auditRepository.findScheduledBetween(today, today.plusDays(withinDays));
        String scope = scopeGuard.currentScopePathPrefix();
        if (scope == null) {
            return audits;
        }
        return audits.stream()
                .filter(a -> a.getScopeOrgNode() == null || a.getScopeOrgNode().getPath().startsWith(scope))
                .toList();
    }

    private static void requireLookahead(int withinDays) {
        if (withinDays < 0 || withinDays > MAX_LOOKAHEAD_DAYS) {
            throw ValidationFailedException.singleField("withinDays", "Must be between 0 and " + MAX_LOOKAHEAD_DAYS);
        }
    }

    private static int percentComplete(long expected, long verified) {
        return expected == 0 ? 100 : (int) Math.round(verified * 100.0 / expected);
    }

    public record AssetSummary(long totalAssets, List<LabelCount> byCategory, List<LabelCount> byStatus) {
    }

    public record AuditCompletion(List<AuditCompletionItem> audits, Integer averagePercentComplete,
                                  List<AuditCompletionItem> recentlyClosed) {
    }

    public record AuditCompletionItem(UUID auditId, String name, String status, int percentComplete,
                                      long exceptionCount) {
    }

    public enum ExpirationKind { WARRANTY, INSURANCE, MAINTENANCE }

    public record Expiration(ExpirationKind kind, UUID assetId, String assetName, LocalDate dueDate, String detail) {
    }
}
