package com.iams.report.application;

import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.FindingStatus;
import com.iams.procurement.domain.PurchaseOrderLine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * EPIC-RPT's own read-side queries - Criteria API for optional filters, the
 * same structural reasoning as DashboardQueries/SecurityEventLogRepositoryImpl.
 */
@Component
public class ReportQueries {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * US-RPT-07: LOCATION_CHANGE history events in a date range, oldest first
     * (an auditor reads relocation activity chronologically), org-scoped via
     * the moved asset's node.
     */
    public List<AssetHistoryEvent> movements(Instant from, Instant to, String scopePathPrefix) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AssetHistoryEvent> query = cb.createQuery(AssetHistoryEvent.class);
        Root<AssetHistoryEvent> root = query.from(AssetHistoryEvent.class);
        root.fetch("asset", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>(List.of(
                cb.equal(root.get("eventType"), AssetHistoryEventType.LOCATION_CHANGE),
                cb.between(root.get("createdAt"), from, to)));
        if (scopePathPrefix != null) {
            predicates.add(cb.like(root.get("asset").get("orgNode").get("path"), scopePathPrefix + "%"));
        }
        query.select(root).where(predicates.toArray(new Predicate[0])).orderBy(cb.asc(root.get("createdAt")));
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * US-RPT-04: every finding that represents a loss or damage event -
     * Missing status, or Verified-but-damaged condition - across all audits,
     * newest first, with audit and asset fetched for the report row. Date
     * range and org scope are optional predicates inside the query.
     */
    public List<AuditFinding> lossFindings(Instant from, Instant to, List<AssetCondition> damageConditions,
                                            String scopePathPrefix) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditFinding> query = cb.createQuery(AuditFinding.class);
        Root<AuditFinding> root = query.from(AuditFinding.class);
        root.fetch("asset", JoinType.INNER);
        root.fetch("audit", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.or(
                cb.equal(root.get("status"), FindingStatus.MISSING),
                root.get("condition").in(damageConditions)));
        // AuditFinding's universal timestamp is verifiedAt (set on persist even for
        // system-classified MISSING rows) - it has no createdAt column by design.
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("verifiedAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThan(root.get("verifiedAt"), to));
        }
        if (scopePathPrefix != null) {
            predicates.add(cb.like(root.get("asset").get("orgNode").get("path"), scopePathPrefix + "%"));
        }
        query.select(root).where(predicates.toArray(new Predicate[0])).orderBy(cb.desc(root.get("verifiedAt")));
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * US-RPT-06: every PO line with its order (and the order's optional vendor
     * link) in a creation-date range - the service groups by vendor and adds
     * subtotals. Procurement is deliberately not org-scoped anywhere in this
     * codebase (POs carry no org node), so no scope predicate exists here.
     */
    public List<PurchaseOrderLine> purchaseOrderLines(Instant from, Instant to) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PurchaseOrderLine> query = cb.createQuery(PurchaseOrderLine.class);
        Root<PurchaseOrderLine> root = query.from(PurchaseOrderLine.class);
        var order = root.fetch("purchaseOrder", JoinType.INNER);
        order.fetch("vendor", JoinType.LEFT);
        List<Predicate> predicates = new ArrayList<>();
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseOrder").get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThan(root.get("purchaseOrder").get("createdAt"), to));
        }
        query.select(root).where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(root.get("purchaseOrder").get("vendorName")),
                        cb.asc(root.get("purchaseOrder").get("poNumber")),
                        cb.asc(root.get("createdAt")));
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * US-RPT-03: latest ASSIGNMENT_CHANGE event per asset, fetched in one
     * query for the whole set rather than one lookup per asset - callers pick
     * the newest row per asset from the descending order.
     */
    public List<AssetHistoryEvent> assignmentEvents(Collection<UUID> assetIds) {
        if (assetIds.isEmpty()) {
            return List.of();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AssetHistoryEvent> query = cb.createQuery(AssetHistoryEvent.class);
        Root<AssetHistoryEvent> root = query.from(AssetHistoryEvent.class);
        query.select(root)
                .where(cb.equal(root.get("eventType"), AssetHistoryEventType.ASSIGNMENT_CHANGE),
                        root.get("asset").get("id").in(assetIds))
                .orderBy(cb.desc(root.get("createdAt")));
        return entityManager.createQuery(query).getResultList();
    }
}
