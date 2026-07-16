package com.iams.search.domain;

import com.iams.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-SRC-04: a user-named asset filter combination (US-SRC-03's parameters).
 * Reference columns carry no FKs on purpose - the AC requires graceful
 * degradation when a referenced entity is later deleted, so existence is
 * checked at resolve time (see SavedSearchService.resolve), never enforced
 * at write time against future deletions.
 */
@Getter
@Setter
@Entity
@Table(name = "saved_search")
public class SavedSearch extends BaseEntity {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String name;

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
