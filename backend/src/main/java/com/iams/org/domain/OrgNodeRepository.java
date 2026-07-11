package com.iams.org.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgNodeRepository extends JpaRepository<OrgNode, UUID> {
}
