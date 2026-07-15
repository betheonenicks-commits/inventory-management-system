package com.iams.dashboard.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardPreferenceRepository extends JpaRepository<DashboardPreference, UUID> {

    Optional<DashboardPreference> findByUserId(UUID userId);
}
