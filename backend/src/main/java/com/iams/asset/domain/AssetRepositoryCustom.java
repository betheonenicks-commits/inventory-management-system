package com.iams.asset.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Filtered/free-text search behind GET /assets - implemented with the
 * JPA Criteria API in AssetRepositoryCustomImpl since Spring Data derived
 * queries can't express "any of these optional filters, plus free text".
 */
public interface AssetRepositoryCustom {

    Page<Asset> search(UUID categoryId, UUID statusId, String query, Pageable pageable);
}
