package com.iams.procurement.domain;

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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-LIF-16: "an immutable lifecycle record" for partial receipt, PO
 * cancellation, and vendor return - append-only, same shape discipline as
 * AssetHistoryEvent/AuditFinding (no version, no updated_* columns, nothing
 * on this class is ever mutated after insert).
 */
@Getter
@Setter
@Entity
@Table(name = "purchase_order_line_event")
public class PurchaseOrderLineEvent {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private PurchaseOrderLine line;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PurchaseOrderLineEventType eventType;

    private Integer quantity;

    @Column(length = 500)
    private String note;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
