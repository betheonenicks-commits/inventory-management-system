package com.iams.inventory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    @Query("SELECT w FROM Warehouse w JOIN FETCH w.orgNode WHERE w.id = :id")
    Optional<Warehouse> findByIdWithOrgNode(UUID id);

    @Query("SELECT w FROM Warehouse w JOIN FETCH w.orgNode ORDER BY w.name ASC")
    List<Warehouse> findAllWithOrgNodeOrderByNameAsc();

    Optional<Warehouse> findByCode(String code);
}
