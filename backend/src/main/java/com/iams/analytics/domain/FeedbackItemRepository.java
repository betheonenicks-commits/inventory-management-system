package com.iams.analytics.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, UUID> {
}
