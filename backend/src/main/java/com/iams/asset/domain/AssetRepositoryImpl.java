package com.iams.asset.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// Deliberately NOT @Repository/any stereotype: Spring Data instantiates fragment
// implementations itself as part of building the repository proxy; a stereotype
// annotation here caused it to be picked up by plain @ComponentScan as an
// independent bean instead, which pre-empted Spring Data's own fragment lookup
// and made it silently fall back to (failing) query derivation for search().
public class AssetRepositoryImpl implements AssetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Asset> search(UUID categoryId, UUID statusId, String query, String locationPathPrefix,
                               String scopePathPrefix, LocalDate purchasedFrom, LocalDate purchasedTo, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Asset> dataQuery = cb.createQuery(Asset.class);
        Root<Asset> root = dataQuery.from(Asset.class);
        // AssetMapper.toResponse reads category/status/orgNode/parentAsset - all LAZY -
        // after this method's transaction has returned, so they must be fetched here or
        // it throws LazyInitializationException (found via live click-testing, same bug
        // class as yesterday's orgScopeNode/Role fixes - see DEVELOPMENT_LOG.md).
        root.fetch("category", JoinType.INNER);
        root.fetch("status", JoinType.INNER);
        root.fetch("orgNode", JoinType.INNER);
        root.fetch("parentAsset", JoinType.LEFT);
        // KNOWN PRE-EXISTING BUG (found 2026-07-20 via US-AUD-20, documented in
        // DEVELOPMENT_LOG.md): when a scope contains an asset that HAS a parent asset,
        // getResultList() hydrates ONE FEWER entity than the query's rows - the child
        // asset is dropped - so getContent().size() < getTotalElements(), and org-scoped
        // searches (and the audits built on them) silently miss it. Diagnosed to the
        // metal 2026-07-20 with SQL logging + direct DB counts: the generated SQL returns
        // the FULL set (verified in psql), so it is purely a Hibernate entity-hydration
        // anomaly on this fetch-join query. Ruled OUT as causes (each tested live, none
        // fixed it): .distinct(true), the self-referential parentAsset LEFT JOIN FETCH,
        // and the Integer.MAX_VALUE page size. The likely real fix is a two-step load - a
        // scalar id projection (SELECT a.id ...) then loadById with an @EntityGraph -
        // rather than SELECT-entity-with-fetch-joins. Left as a focused follow-up, not a
        // rushed change to this shared, critical query. Meanwhile callers needing an exact
        // scope count resolve via the same getContent() path so preview == reality
        // (see AuditService.sampleSizePreview).
        dataQuery.select(root).distinct(true).where(buildPredicates(cb, root, categoryId, statusId, query,
                locationPathPrefix, scopePathPrefix, purchasedFrom, purchasedTo));
        if (pageable.getSort().isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                var path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            dataQuery.orderBy(orders);
        } else {
            dataQuery.orderBy(cb.desc(root.get("createdAt")));
        }

        var typedQuery = entityManager.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());
        List<Asset> content = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Asset> countRoot = countQuery.from(Asset.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(cb, countRoot, categoryId, statusId, query,
                locationPathPrefix, scopePathPrefix, purchasedFrom, purchasedTo));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Asset> root, UUID categoryId, UUID statusId,
                                         String query, String locationPathPrefix, String scopePathPrefix,
                                         LocalDate purchasedFrom, LocalDate purchasedTo) {
        List<Predicate> predicates = new ArrayList<>();
        if (categoryId != null) {
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
        }
        if (statusId != null) {
            predicates.add(cb.equal(root.get("status").get("id"), statusId));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.toLowerCase() + "%";
            Predicate nameMatch = cb.like(cb.lower(root.get("name")), like);
            Predicate assetNumberMatch = cb.like(cb.lower(root.get("assetNumber")), like);
            Predicate serialMatch = cb.like(cb.lower(cb.coalesce(root.get("serialNumber"), "")), like);
            // US-SRC-05: rfidTagId is searchable from day one - empty today, so it
            // simply never matches until an RFID rollout populates it.
            Predicate rfidMatch = cb.like(cb.lower(cb.coalesce(root.get("rfidTagId"), "")), like);
            predicates.add(cb.or(nameMatch, assetNumberMatch, serialMatch, rfidMatch));
        }
        if (locationPathPrefix != null) {
            // US-SRC-03: the caller's requested location filter - independent of,
            // and ANDed with, the mandatory scope restriction below.
            predicates.add(cb.like(root.get("orgNode").get("path"), locationPathPrefix + "%"));
        }
        if (scopePathPrefix != null) {
            // FR-USR-04: scope node itself or any descendant - see OrgScopeGuard.
            predicates.add(cb.like(root.get("orgNode").get("path"), scopePathPrefix + "%"));
        }
        if (purchasedFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), purchasedFrom));
        }
        if (purchasedTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), purchasedTo));
        }
        return predicates.toArray(new Predicate[0]);
    }
}
