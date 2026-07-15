package com.iams.asset.domain;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Filtered/free-text search behind GET /assets - implemented with the
 * JPA Criteria API in AssetRepositoryCustomImpl since Spring Data derived
 * queries can't express "any of these optional filters, plus free text".
 */
public interface AssetRepositoryCustom {

    /**
     * scopePathPrefix, when non-null, restricts results to assets whose
     * orgNode.path starts with it - the scope node itself or any descendant
     * (FR-USR-04). Null means unrestricted - callers resolve that via
     * OrgScopeGuard.currentScopePathPrefix(), not here.
     * <p>
     * locationPathPrefix (US-SRC-03) is the caller's *requested* location
     * filter, applied alongside (AND) the scope prefix - two independent
     * subtree restrictions compose correctly whether or not one nests inside
     * the other. purchasedFrom/purchasedTo bound purchaseDate inclusively.
     */
    Page<Asset> search(UUID categoryId, UUID statusId, String query, String locationPathPrefix,
                        String scopePathPrefix, LocalDate purchasedFrom, LocalDate purchasedTo, Pageable pageable);
}
