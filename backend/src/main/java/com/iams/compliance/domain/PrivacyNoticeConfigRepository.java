package com.iams.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyNoticeConfigRepository extends JpaRepository<PrivacyNoticeConfig, UUID> {

    Optional<PrivacyNoticeConfig> findByFieldName(String fieldName);

    List<PrivacyNoticeConfig> findAllByOrderByFieldNameAsc();
}
