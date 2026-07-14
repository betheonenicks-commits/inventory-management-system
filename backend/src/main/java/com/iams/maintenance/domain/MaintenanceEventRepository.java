package com.iams.maintenance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceEventRepository extends JpaRepository<MaintenanceEvent, UUID> {

    @Query("SELECT e FROM MaintenanceEvent e JOIN FETCH e.asset LEFT JOIN FETCH e.schedule WHERE e.id = :id")
    Optional<MaintenanceEvent> findByIdWithAssociations(UUID id);

    @Query("SELECT e FROM MaintenanceEvent e JOIN FETCH e.asset LEFT JOIN FETCH e.schedule "
            + "WHERE e.asset.id = :assetId ORDER BY e.performedAt DESC")
    List<MaintenanceEvent> findByAssetIdWithAssociationsOrderByPerformedAtDesc(UUID assetId);

    @Query("SELECT e FROM MaintenanceEvent e JOIN FETCH e.asset LEFT JOIN FETCH e.schedule "
            + "WHERE e.maintenanceType = :type ORDER BY e.performedAt DESC")
    List<MaintenanceEvent> findByMaintenanceTypeWithAssociationsOrderByPerformedAtDesc(MaintenanceType type);

    @Query("SELECT e FROM MaintenanceEvent e JOIN FETCH e.asset LEFT JOIN FETCH e.schedule ORDER BY e.performedAt DESC")
    List<MaintenanceEvent> findAllWithAssociationsOrderByPerformedAtDesc();
}
