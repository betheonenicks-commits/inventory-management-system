package com.iams.dashboard.api.dto;

import com.iams.dashboard.domain.DashboardTile;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * EPIC-DSH's response/request records, grouped in one file since each is a
 * few lines and they only ever travel together through DashboardController.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record LabelCountResponse(String label, long count) {
    }

    public record AssetSummaryResponse(long totalAssets, List<LabelCountResponse> byCategory,
                                        List<LabelCountResponse> byStatus) {
    }

    public record AuditCompletionItemResponse(UUID auditId, String name, String status, int percentComplete,
                                              long exceptionCount) {
    }

    /**
     * averagePercentComplete is null when no active audits exist - the AC-DSH-02
     * empty state, not a zero. recentlyClosed is the US-AUD-17 "recent audits"
     * half of the same dashboard.
     */
    public record AuditCompletionResponse(List<AuditCompletionItemResponse> audits, Integer averagePercentComplete,
                                          List<AuditCompletionItemResponse> recentlyClosed) {
    }

    public record ExpirationResponse(String kind, UUID assetId, String assetName, LocalDate dueDate, String detail) {
    }

    public record LowStockResponse(UUID itemId, String name, String sku, String unitOfMeasure,
                                    BigDecimal totalQuantity, BigDecimal reorderLevel) {
    }

    public record ActivityFeedEntryResponse(UUID eventId, String eventType, UUID assetId, String assetName,
                                             String fieldName, String oldValue, String newValue, UUID actorId,
                                             Instant occurredAt) {
    }

    public record AuditCalendarEntryResponse(UUID auditId, String name, String status, LocalDate scheduledDate) {
    }

    public record PreferencesResponse(List<DashboardTile> tiles, boolean configured,
                                       List<DashboardTile> availableTiles) {
    }

    /** Unknown tile names are rejected at deserialization (400) because the list is typed to the enum. */
    public record PreferencesUpdateRequest(@NotNull List<DashboardTile> tiles) {
    }
}
