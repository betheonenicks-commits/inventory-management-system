package com.iams.dashboard.api;

import com.iams.dashboard.api.dto.DashboardDtos.ActivityFeedEntryResponse;
import com.iams.dashboard.api.dto.DashboardDtos.AssetSummaryResponse;
import com.iams.dashboard.api.dto.DashboardDtos.AuditCalendarEntryResponse;
import com.iams.dashboard.api.dto.DashboardDtos.AuditCompletionItemResponse;
import com.iams.dashboard.api.dto.DashboardDtos.AuditCompletionResponse;
import com.iams.dashboard.api.dto.DashboardDtos.ExpirationResponse;
import com.iams.dashboard.api.dto.DashboardDtos.LabelCountResponse;
import com.iams.dashboard.api.dto.DashboardDtos.LowStockResponse;
import com.iams.dashboard.api.dto.DashboardDtos.PreferencesResponse;
import com.iams.dashboard.api.dto.DashboardDtos.PreferencesUpdateRequest;
import com.iams.dashboard.application.DashboardPreferenceService;
import com.iams.dashboard.application.DashboardQueries.LabelCount;
import com.iams.dashboard.application.DashboardService;
import com.iams.dashboard.domain.DashboardTile;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EPIC-DSH (BR-08). Every endpoint is gated on dashboards:read - the
 * permission V15 seeded onto VIEWER, granted to the other dashboard-facing
 * roles in V39 - not on each source module's own read permission; see
 * DashboardTile's Javadoc for the aggregate-vs-detail reasoning. Preferences
 * PUT is also dashboards:read (not a :write) deliberately: it only ever
 * writes the caller's own layout row, never shared state.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardPreferenceService preferenceService;

    public DashboardController(DashboardService dashboardService, DashboardPreferenceService preferenceService) {
        this.dashboardService = dashboardService;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/asset-summary")
    @PreAuthorize("@perm.has('dashboards:read')")
    public AssetSummaryResponse assetSummary() {
        DashboardService.AssetSummary summary = dashboardService.assetSummary();
        return new AssetSummaryResponse(summary.totalAssets(), toLabelCounts(summary.byCategory()),
                toLabelCounts(summary.byStatus()));
    }

    @GetMapping("/audit-completion")
    @PreAuthorize("@perm.has('dashboards:read')")
    public AuditCompletionResponse auditCompletion() {
        DashboardService.AuditCompletion completion = dashboardService.auditCompletion();
        return new AuditCompletionResponse(completion.audits().stream()
                .map(a -> new AuditCompletionItemResponse(a.auditId(), a.name(), a.status(), a.percentComplete()))
                .toList(), completion.averagePercentComplete());
    }

    @GetMapping("/expirations")
    @PreAuthorize("@perm.has('dashboards:read')")
    public List<ExpirationResponse> expirations(@RequestParam(defaultValue = "30") int withinDays) {
        return dashboardService.expirations(withinDays).stream()
                .map(e -> new ExpirationResponse(e.kind().name(), e.assetId(), e.assetName(), e.dueDate(), e.detail()))
                .toList();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("@perm.has('dashboards:read')")
    public List<LowStockResponse> lowStock() {
        return dashboardService.lowStock().stream()
                .map(low -> new LowStockResponse(low.item().getId(), low.item().getName(), low.item().getSku(),
                        low.item().getUnitOfMeasure().name(), low.totalQuantity(), low.item().getReorderLevel()))
                .toList();
    }

    @GetMapping("/activity-feed")
    @PreAuthorize("@perm.has('dashboards:read')")
    public List<ActivityFeedEntryResponse> activityFeed(@RequestParam(defaultValue = "20") int limit) {
        return dashboardService.activityFeed(limit).stream()
                .map(e -> new ActivityFeedEntryResponse(e.getId(), e.getEventType().name(), e.getAsset().getId(),
                        e.getAsset().getName(), e.getFieldName(), e.getOldValue(), e.getNewValue(), e.getCreatedBy(),
                        e.getCreatedAt()))
                .toList();
    }

    @GetMapping("/audit-calendar")
    @PreAuthorize("@perm.has('dashboards:read')")
    public List<AuditCalendarEntryResponse> auditCalendar(@RequestParam(defaultValue = "30") int withinDays) {
        return dashboardService.auditCalendar(withinDays).stream()
                .map(a -> new AuditCalendarEntryResponse(a.getId(), a.getName(), a.getStatus().name(),
                        a.getScheduledDate()))
                .toList();
    }

    @GetMapping("/preferences")
    @PreAuthorize("@perm.has('dashboards:read')")
    public PreferencesResponse preferences() {
        return toResponse(preferenceService.current());
    }

    @PutMapping("/preferences")
    @PreAuthorize("@perm.has('dashboards:read')")
    public PreferencesResponse savePreferences(@Valid @RequestBody PreferencesUpdateRequest request) {
        return toResponse(preferenceService.save(request.tiles()));
    }

    private static PreferencesResponse toResponse(DashboardPreferenceService.Preferences prefs) {
        return new PreferencesResponse(prefs.tiles(), prefs.configured(), List.of(DashboardTile.values()));
    }

    private static List<LabelCountResponse> toLabelCounts(List<LabelCount> counts) {
        return counts.stream().map(c -> new LabelCountResponse(c.label(), c.count())).toList();
    }
}
