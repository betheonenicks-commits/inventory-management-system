package com.iams.asset.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetRepository extends JpaRepository<Asset, UUID>, AssetRepositoryCustom {

    /**
     * category/status/orgNode/parentAsset are all FetchType.LAZY and open-in-view is
     * disabled, so a plain findById() returns proxies that throw
     * LazyInitializationException once AssetMapper (called after the transactional
     * service method has already returned) reads them - found via live click-testing,
     * the same bug class as AppUserRepository.findByIdWithOrgScopeNode (see its
     * comment). AssetQueryService.get()/history()/movements() all need the mapped
     * response and use this; nothing here needs the plain lazy findById() instead.
     */
    @Query("SELECT a FROM Asset a JOIN FETCH a.category JOIN FETCH a.status JOIN FETCH a.orgNode "
            + "LEFT JOIN FETCH a.parentAsset WHERE a.id = :id")
    Optional<Asset> findByIdWithAssociations(UUID id);

    /** Same reasoning as findByIdWithAssociations - AssetHierarchyController maps every child returned here. */
    @Query("SELECT a FROM Asset a JOIN FETCH a.category JOIN FETCH a.status JOIN FETCH a.orgNode "
            + "LEFT JOIN FETCH a.parentAsset WHERE a.parentAsset.id = :parentId ORDER BY a.createdAt ASC")
    List<Asset> findByParentAssetIdWithAssociationsOrderByCreatedAtAsc(UUID parentId);

    boolean existsByCategoryId(UUID categoryId);

    /** US-ORG-01 delete-block AC: is any asset still scoped to this org node. */
    boolean existsByOrgNodeId(UUID orgNodeId);

    boolean existsByParentAssetId(UUID parentAssetId);

    /** US-USR-08: assets currently assigned to a person, to block offboarding while any remain. */
    List<Asset> findByAssignedToPersonId(UUID personId);
}
