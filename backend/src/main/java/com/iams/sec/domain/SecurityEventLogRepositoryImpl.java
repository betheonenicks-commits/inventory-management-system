package com.iams.sec.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

// Deliberately NOT @Repository/any stereotype - see AssetRepositoryImpl's identical
// comment; the same Spring Data fragment-lookup conflict applies here.
public class SecurityEventLogRepositoryImpl implements SecurityEventLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<SecurityEventLog> search(UUID actorUserId, SecurityEventType eventType, Instant from, Instant to, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<SecurityEventLog> dataQuery = cb.createQuery(SecurityEventLog.class);
        Root<SecurityEventLog> root = dataQuery.from(SecurityEventLog.class);
        dataQuery.select(root).where(buildPredicates(cb, root, actorUserId, eventType, from, to));
        dataQuery.orderBy(cb.desc(root.get("createdAt")));

        List<SecurityEventLog> content = entityManager.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SecurityEventLog> countRoot = countQuery.from(SecurityEventLog.class);
        countQuery.select(cb.count(countRoot)).where(buildPredicates(cb, countRoot, actorUserId, eventType, from, to));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<SecurityEventLog> root, UUID actorUserId,
                                         SecurityEventType eventType, Instant from, Instant to) {
        List<Predicate> predicates = new ArrayList<>();
        if (actorUserId != null) {
            predicates.add(cb.equal(root.get("actorUserId"), actorUserId));
        }
        if (eventType != null) {
            predicates.add(cb.equal(root.get("eventType"), eventType));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return predicates.toArray(new Predicate[0]);
    }
}
