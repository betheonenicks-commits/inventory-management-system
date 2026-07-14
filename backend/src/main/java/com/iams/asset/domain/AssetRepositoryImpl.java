package com.iams.asset.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
    public Page<Asset> search(UUID categoryId, UUID statusId, String query, String scopePathPrefix, Pageable pageable) {
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
        dataQuery.select(root).distinct(true).where(buildPredicates(cb, root, categoryId, statusId, query, scopePathPrefix));
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
        countQuery.select(cb.count(countRoot)).where(buildPredicates(cb, countRoot, categoryId, statusId, query, scopePathPrefix));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Asset> root, UUID categoryId, UUID statusId,
                                         String query, String scopePathPrefix) {
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
            predicates.add(cb.or(nameMatch, assetNumberMatch, serialMatch));
        }
        if (scopePathPrefix != null) {
            // FR-USR-04: scope node itself or any descendant - see OrgScopeGuard.
            predicates.add(cb.like(root.get("orgNode").get("path"), scopePathPrefix + "%"));
        }
        return predicates.toArray(new Predicate[0]);
    }
}
