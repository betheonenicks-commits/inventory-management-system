package com.iams.org.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {

    List<Person> findAllByOrderByFullNameAsc();

    List<Person> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(String fullName);

    /** US-ORG-01 delete-block AC: is any person still scoped to this org node. */
    boolean existsByOrgNodeId(UUID orgNodeId);
}
