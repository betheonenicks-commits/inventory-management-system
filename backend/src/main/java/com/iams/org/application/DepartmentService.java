package com.iams.org.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-ORG-03: departments/cost centers as their own dimension, independent of
 * the physical org_node hierarchy.
 * <p>
 * AC-ORG-03-X: {@link #delete} blocks while any asset still names this
 * department as its custodian (the {@code asset.assigned_to_department_id} FK
 * added with US-LIF-04), returning the dependent list rather than letting the
 * database FK constraint surface as a raw 500. The dependent-<em>person</em>
 * half of AC-ORG-03-H is still open: Person does not yet reference a Department.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserProvider currentUserProvider;

    public DepartmentService(DepartmentRepository departmentRepository, AssetRepository assetRepository,
                             CurrentUserProvider currentUserProvider) {
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<Department> list() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Department get(UUID id) {
        return departmentRepository.findById(id).orElseThrow(() -> NotFoundException.of("Department", id));
    }

    @Transactional
    public Department create(String name, String costCenterCode) {
        if (departmentRepository.existsByCostCenterCode(costCenterCode)) {
            throw ValidationFailedException.singleField("costCenterCode", "This cost center code is already in use");
        }
        Department department = new Department();
        department.setName(name);
        department.setCostCenterCode(costCenterCode);
        department.setActive(true);
        department.setCreatedBy(currentUserProvider.current().id());
        return departmentRepository.save(department);
    }

    @Transactional
    public Department update(UUID id, String name, String costCenterCode, Boolean active, long expectedVersion) {
        Department department = get(id);
        if (department.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, department.getVersion(), department);
        }
        if (name != null) {
            department.setName(name);
        }
        if (costCenterCode != null && !costCenterCode.equals(department.getCostCenterCode())) {
            if (departmentRepository.existsByCostCenterCode(costCenterCode)) {
                throw ValidationFailedException.singleField("costCenterCode", "This cost center code is already in use");
            }
            department.setCostCenterCode(costCenterCode);
        }
        if (active != null) {
            department.setActive(active);
        }
        department.setUpdatedBy(currentUserProvider.current().id());
        try {
            return departmentRepository.saveAndFlush(department);
        } catch (OptimisticLockingFailureException e) {
            Department current = get(id);
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Department department = get(id);

        // AC-ORG-03-X: block while assets still name this department as custodian,
        // returning the dependent list (same structured shape as US-USR-08's
        // offboarding block) instead of letting the FK constraint 500.
        List<Asset> blockingAssets = assetRepository.findByAssignedToDepartmentId(id);
        if (!blockingAssets.isEmpty()) {
            throw blockedByAssignedAssets(department, blockingAssets);
        }

        departmentRepository.delete(department);
    }

    private ConflictException blockedByAssignedAssets(Department department, List<Asset> blockingAssets) {
        List<Map<String, Object>> payload = blockingAssets.stream().map(asset -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("assetId", asset.getId());
            entry.put("assetNumber", asset.getAssetNumber());
            entry.put("name", asset.getName());
            return entry;
        }).toList();

        Map<String, Object> extraProperties = new LinkedHashMap<>();
        extraProperties.put("blockingAssets", payload);
        extraProperties.put("resolutionActions", List.of(
                "POST /api/v1/assets/{assetId}/assignment/department — reassign to another department",
                "POST /api/v1/assets/{assetId}/assignment — reassign to a person",
                "DELETE /api/v1/assets/{assetId}/assignment — unassign"));

        return new ConflictException(
                "DEPARTMENT_HAS_ASSIGNED_ASSETS",
                "Cannot delete department with assigned assets",
                blockingAssets.size() + " asset(s) are currently assigned to department '" + department.getName()
                        + "' and must be reassigned or unassigned before it can be deleted.",
                extraProperties);
    }
}
