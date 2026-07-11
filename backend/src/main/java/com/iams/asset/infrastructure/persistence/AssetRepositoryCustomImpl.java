package com.iams.asset.infrastructure.persistence;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRepositoryCustomImpl implements AssetRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Asset> search(UUID categoryId, UUID statusId, String query, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Asset> dataQuery = cb.createQuery(Asset.class);
        Root<Asset> root = dataQuery.from(Asset.class);
        dataQuery.select(root).where(buildPredicates(cb, root, categoryId, statusId, query));
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
        countQuery.select(cb.count(countRoot)).where(buildPredicates(cb, countRoot, categoryId, statusId, query));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<Asset> root, UUID categoryId, UUID statusId, String query) {
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
        return predicates.toArray(new Predicate[0]);
    }
}
