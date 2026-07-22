package com.iams.migration.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunRepository extends JpaRepository<ImportRun, UUID> {

    /** US-MIG-04: import history, newest first. */
    List<ImportRun> findAllByOrderByCreatedAtDesc();

    /** Idempotency guard (AC-MIG-03-H): find a run already committed under a given key. */
    Optional<ImportRun> findByIdempotencyKey(String idempotencyKey);
}
