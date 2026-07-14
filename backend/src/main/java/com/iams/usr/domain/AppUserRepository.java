package com.iams.usr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    boolean existsByUsername(String username);

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAllByOrderByDisplayNameAsc();

    Optional<AppUser> findByPersonId(UUID personId);

    /** US-ORG-01 delete-block AC: is any user still scoped to this org node. */
    boolean existsByOrgScopeNodeId(UUID orgNodeId);

    /**
     * orgScopeNode is FetchType.LAZY and open-in-view is disabled, so a plain
     * findById() returns a proxy that throws LazyInitializationException the
     * moment UserMapper (called after the transactional service method has
     * already returned) touches .getName() on it. UserQueryService.get()/
     * list() use these JOIN FETCH variants specifically because they're
     * always followed by mapping to a response DTO; UserScopeResolver and
     * AuthController's login path don't need org-scope-node data and keep
     * using the plain findById()/findByUsername() above.
     */
    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.orgScopeNode WHERE u.id = :id")
    Optional<AppUser> findByIdWithOrgScopeNode(UUID id);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.orgScopeNode ORDER BY u.displayName ASC")
    List<AppUser> findAllWithOrgScopeNodeOrderByDisplayNameAsc();
}
