package com.iams.dashboard.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetInsuranceDetail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * EPIC-DSH's read-side aggregate queries. Criteria API, not JPQL string
 * templates, for the same reason SecurityEventLogRepositoryImpl switched: an
 * optional filter (here, the caller's org-scope path prefix) that's simply
 * absent from the query when null never hands PGJDBC an ambiguous placeholder.
 * Scalar tuples, not entities, wherever a widget only needs a few columns -
 * a dashboard that loads six widgets shouldn't hydrate six object graphs.
 */
@Component
public class DashboardQueries {

    @PersistenceContext
    private EntityManager entityManager;

    /** US-DSH-01: total asset count within scope. */
    public long assetCount(String scopePathPrefix) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Asset> root = query.from(Asset.class);
        query.select(cb.count(root)).where(scopePredicates(cb, root, scopePathPrefix));
        return entityManager.createQuery(query).getSingleResult();
    }

    /** US-DSH-01: per-category asset counts within scope, largest first. */
    public List<LabelCount> assetCountByCategory(String scopePathPrefix) {
        return groupedAssetCount("category", "name", scopePathPrefix);
    }

    /** US-DSH-01: per-status asset counts within scope, largest first. */
    public List<LabelCount> assetCountByStatus(String scopePathPrefix) {
        // AssetStatusDef's display attribute is "label", not "name" (its "code" is the machine key).
        return groupedAssetCount("status", "label", scopePathPrefix);
    }

    private List<LabelCount> groupedAssetCount(String association, String labelAttribute, String scopePathPrefix) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Asset> root = query.from(Asset.class);
        var label = root.join(association, JoinType.INNER).<String>get(labelAttribute);
        query.multiselect(label, cb.count(root))
                .where(scopePredicates(cb, root, scopePathPrefix))
                .groupBy(label)
                .orderBy(cb.desc(cb.count(root)), cb.asc(label));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new LabelCount(t.get(0, String.class), t.get(1, Long.class)))
                .toList();
    }

    /** US-DSH-03: assets whose warranty ends inside the lookahead window, scoped. */
    public List<ExpiringEntry> warrantyExpirations(LocalDate from, LocalDate to, String scopePathPrefix) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Asset> root = query.from(Asset.class);
        List<Predicate> predicates = new ArrayList<>(List.of(cb.between(root.get("warrantyEndDate"), from, to)));
        addScopePredicate(predicates, cb, root, scopePathPrefix);
        query.multiselect(root.get("id"), root.get("name"), root.get("warrantyEndDate"))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(root.get("warrantyEndDate")));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new ExpiringEntry(t.get(0, UUID.class), t.get(1, String.class), t.get(2, LocalDate.class), null))
                .toList();
    }

    /** US-DSH-03: insurance policies expiring inside the lookahead window, scoped via the covered asset. */
    public List<ExpiringEntry> insuranceExpirations(LocalDate from, LocalDate to, String scopePathPrefix) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<AssetInsuranceDetail> root = query.from(AssetInsuranceDetail.class);
        var asset = root.join("asset", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>(List.of(cb.between(root.get("policyExpiryDate"), from, to)));
        if (scopePathPrefix != null) {
            predicates.add(cb.like(asset.join("orgNode", JoinType.INNER).get("path"), scopePathPrefix + "%"));
        }
        query.multiselect(asset.get("id"), asset.get("name"), root.get("policyExpiryDate"), root.get("insurerName"))
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(root.get("policyExpiryDate")));
        return entityManager.createQuery(query).getResultList().stream()
                .map(t -> new ExpiringEntry(t.get(0, UUID.class), t.get(1, String.class), t.get(2, LocalDate.class),
                        t.get(3, String.class)))
                .toList();
    }

    /**
     * US-DSH-05: most recent asset-history events within scope, newest first.
     * The scope predicate is part of the query (not post-filtered) so a scoped
     * caller's feed is never under-filled by rows that were fetched and then
     * dropped.
     */
    public List<AssetHistoryEvent> recentActivity(String scopePathPrefix, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AssetHistoryEvent> query = cb.createQuery(AssetHistoryEvent.class);
        Root<AssetHistoryEvent> root = query.from(AssetHistoryEvent.class);
        // The mapper reads event.getAsset().getName() after the service transaction
        // returns - fetch it here or it's a LazyInitializationException (the same
        // bug class AssetRepositoryImpl's own comment documents).
        root.fetch("asset", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();
        if (scopePathPrefix != null) {
            predicates.add(cb.like(root.get("asset").get("orgNode").get("path"), scopePathPrefix + "%"));
        }
        query.select(root).where(predicates.toArray(new Predicate[0])).orderBy(cb.desc(root.get("createdAt")));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }

    private Predicate[] scopePredicates(CriteriaBuilder cb, Root<Asset> root, String scopePathPrefix) {
        List<Predicate> predicates = new ArrayList<>();
        addScopePredicate(predicates, cb, root, scopePathPrefix);
        return predicates.toArray(new Predicate[0]);
    }

    private void addScopePredicate(List<Predicate> predicates, CriteriaBuilder cb, Root<Asset> root, String scopePathPrefix) {
        if (scopePathPrefix != null) {
            // FR-USR-04 / US-DSH-07: scope node itself or any descendant, same
            // path-prefix rule as OrgScopeGuard/AssetRepositoryImpl.
            predicates.add(cb.like(root.get("orgNode").get("path"), scopePathPrefix + "%"));
        }
    }

    public record LabelCount(String label, long count) {
    }

    public record ExpiringEntry(UUID assetId, String assetName, LocalDate dueDate, String detail) {
    }
}
