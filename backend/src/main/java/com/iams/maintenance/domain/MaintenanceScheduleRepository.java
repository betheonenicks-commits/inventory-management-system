package com.iams.maintenance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, UUID> {

    @Query("SELECT s FROM MaintenanceSchedule s JOIN FETCH s.asset WHERE s.id = :id")
    Optional<MaintenanceSchedule> findByIdWithAsset(UUID id);

    @Query("SELECT s FROM MaintenanceSchedule s JOIN FETCH s.asset WHERE s.asset.id = :assetId ORDER BY s.nextDueDate ASC")
    List<MaintenanceSchedule> findByAssetIdWithAssetOrderByNextDueDateAsc(UUID assetId);

    @Query("SELECT s FROM MaintenanceSchedule s JOIN FETCH s.asset WHERE s.active = true AND s.nextDueDate <= :onOrBefore ORDER BY s.nextDueDate ASC")
    List<MaintenanceSchedule> findDueOnOrBefore(LocalDate onOrBefore);

    @Query("SELECT s FROM MaintenanceSchedule s JOIN FETCH s.asset ORDER BY s.nextDueDate ASC")
    List<MaintenanceSchedule> findAllWithAssetOrderByNextDueDateAsc();
}
