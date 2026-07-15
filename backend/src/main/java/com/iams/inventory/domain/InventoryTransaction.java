package com.iams.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-INV-02: the immutable movement ledger - every quantity change to
 * {@link InventoryStockBalance} is derived from exactly one row here, appended
 * never edited (no version, no updated_* columns - same discipline as
 * {@code AssetHistoryEvent}/{@code AuditFinding}). {@code linkedTransactionId}
 * pairs a TRANSFER_OUT with its TRANSFER_IN counterpart (US-INV-08's "atomic
 * linked pair" - both rows are written in one {@code @Transactional} method,
 * so a failure partway through rolls back both, never one without the other).
 * <p>
 * currencyCode/fxRate/reportingUnitCost (US-INV-10) are only ever populated on
 * a STOCK_IN row - captured once at entry from whatever was submitted, never
 * recalculated later against a different rate.
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false, updatable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @Column(name = "sub_location", nullable = false, updatable = false)
    private String subLocation = "";

    @Column(name = "lot_number", nullable = false, updatable = false)
    private String lotNumber = "";

    @Column(name = "expiry_date", updatable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private InventoryTransactionType transactionType;

    /** Always positive - direction is expressed by transactionType, not the sign. */
    @Column(nullable = false, updatable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", updatable = false, precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "currency_code", updatable = false, length = 3)
    private String currencyCode;

    @Column(name = "fx_rate", updatable = false, precision = 14, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "reporting_unit_cost", updatable = false, precision = 14, scale = 4)
    private BigDecimal reportingUnitCost;

    @Column(name = "reason_code", nullable = false, updatable = false)
    private String reasonCode;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "performed_by_user_id", nullable = false, updatable = false)
    private UUID performedByUserId;

    @Column(name = "performed_by_username", nullable = false, updatable = false)
    private String performedByUsername;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "linked_transaction_id", updatable = false)
    private UUID linkedTransactionId;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }
}
