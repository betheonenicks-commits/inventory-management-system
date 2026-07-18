package com.iams.report.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-RPT-15: a user-built report definition - an ordered field selection plus
 * the US-SRC-03 filter set. Reference columns carry no FKs (same reasoning as
 * SavedSearch): the AC requires a definition referencing a since-removed
 * field or entity to degrade at run time, not to block or cascade the removal.
 */
@Getter
@Setter
@Entity
@Table(name = "adhoc_report")
public class AdHocReport extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String name;

    /** Ordered field-catalog keys; "custom:<fieldKey>" rows may outlive their definition. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> fields = new ArrayList<>();

    @Column(length = 255)
    private String query;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "category_id")
    private UUID categoryId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "status_id")
    private UUID statusId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "org_node_id")
    private UUID orgNodeId;

    @Column(name = "purchased_from")
    private LocalDate purchasedFrom;

    @Column(name = "purchased_to")
    private LocalDate purchasedTo;
}
