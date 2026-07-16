package com.iams.search.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {

    List<SavedSearch> findByUserIdOrderByNameAsc(UUID userId);

    Optional<SavedSearch> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
