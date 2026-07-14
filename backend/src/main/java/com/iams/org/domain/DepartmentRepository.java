package com.iams.org.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByCostCenterCode(String costCenterCode);

    List<Department> findAllByOrderByNameAsc();
}
