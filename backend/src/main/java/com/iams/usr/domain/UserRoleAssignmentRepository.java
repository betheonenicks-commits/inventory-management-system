package com.iams.usr.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

    /**
     * Fetches the Role in the same query - callers (UserQueryService.get()/
     * getByUsername()) read Role.getCode()/getPermissions() after this
     * method's transaction has returned, which throws LazyInitializationException
     * against a lazy-only Role proxy since open-in-view is disabled.
     */
    @Query("SELECT a FROM UserRoleAssignment a JOIN FETCH a.role WHERE a.user.id = :userId")
    List<UserRoleAssignment> findByUserId(UUID userId);

    /** Batch form of findByUserId, for the same reason - used by UserQueryService.list(). */
    @Query("SELECT a FROM UserRoleAssignment a JOIN FETCH a.role WHERE a.user.id IN :userIds")
    List<UserRoleAssignment> findByUserIdIn(Collection<UUID> userIds);

    /** US-USR-02 delete-block AC: who is currently assigned this role, if anyone. */
    List<UserRoleAssignment> findByRoleId(UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
