package com.iams.maintenance.domain;

import com.iams.asset.domain.Asset;
import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-LIF-06: an asset sent out for repair and its eventual return.
 * previousStatusCode is captured at open time (not assumed to be IN_STORAGE,
 * unlike DisposalService's restore) since a repair can be logged from IN_USE
 * just as easily - "status reverts appropriately" (AC-LIF-06-H) means back to
 * whatever it actually was, not a fixed status.
 */
@Getter
@Setter
@Entity
@Table(name = "repair_event")
public class RepairEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "previous_status_code", nullable = false, length = 30)
    private String previousStatusCode;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    @Column(name = "actual_return_date")
    private LocalDate actualReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RepairEventStatus status = RepairEventStatus.OPEN;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "logged_by", nullable = false)
    private UUID loggedBy;
}
