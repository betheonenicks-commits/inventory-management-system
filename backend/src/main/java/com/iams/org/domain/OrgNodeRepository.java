package com.iams.org.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrgNodeRepository extends JpaRepository<OrgNode, UUID> {

    boolean existsByCode(String code);

    /** US-ORG-01 delete-block AC: does this node still have children. */
    boolean existsByParentId(UUID parentId);

    /**
     * level/parent are FetchType.LAZY and open-in-view is disabled, so a plain
     * findById()/findAll() returns a proxy that throws LazyInitializationException
     * once OrgNodeMapper (called after this transaction returns) reads
     * .getName() on either. Only used where the caller maps to a response DTO;
     * OrgScopeGuard's path lookups don't need level/parent and use the plain
     * findById() below instead.
     */
    @Query("SELECT n FROM OrgNode n JOIN FETCH n.level LEFT JOIN FETCH n.parent ORDER BY n.path")
    List<OrgNode> findAllWithLevelOrderByPath();

    @Query("SELECT n FROM OrgNode n JOIN FETCH n.level LEFT JOIN FETCH n.parent WHERE n.id = :id")
    Optional<OrgNode> findByIdWithLevel(UUID id);
}
