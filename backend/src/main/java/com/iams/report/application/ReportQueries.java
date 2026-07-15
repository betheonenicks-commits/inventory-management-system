package com.iams.report.application;

import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventType;
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
