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
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
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
 * AC-ORG-03-X: {@link #delete} blocks while any asset or person still references
 * this department (the {@code asset.assigned_to_department_id} FK from US-LIF-04
 * and {@code person.department_id} from US-ORG-03), returning the dependent list
 * rather than letting the database FK constraint surface as a raw 500.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final PersonRepository personRepository;
    private final CurrentUserProvider currentUserProvider;

    public DepartmentService(DepartmentRepository departmentRepository, AssetRepository assetRepository,
                             PersonRepository personRepository, CurrentUserProvider currentUserProvider) {
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.personRepository = personRepository;
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

        // AC-ORG-03-X: block while any asset or person still references this
        // department, returning the dependent list (same structured shape as
        // US-USR-08's offboarding block) instead of letting the FK constraint 500.
        List<Asset> blockingAssets = assetRepository.findByAssignedToDepartmentId(id);
        List<Person> blockingPersons = personRepository.findByDepartmentId(id);
        if (!blockingAssets.isEmpty() || !blockingPersons.isEmpty()) {
            throw blockedByDependents(department, blockingAssets, blockingPersons);
        }

        departmentRepository.delete(department);
    }

    private ConflictException blockedByDependents(Department department, List<Asset> blockingAssets,
                                                  List<Person> blockingPersons) {
        List<Map<String, Object>> assetPayload = blockingAssets.stream().map(asset -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("assetId", asset.getId());
            entry.put("assetNumber", asset.getAssetNumber());
            entry.put("name", asset.getName());
            return entry;
        }).toList();
        List<Map<String, Object>> personPayload = blockingPersons.stream().map(person -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("personId", person.getId());
            entry.put("fullName", person.getFullName());
            return entry;
        }).toList();

        Map<String, Object> extraProperties = new LinkedHashMap<>();
        extraProperties.put("blockingAssets", assetPayload);
        extraProperties.put("blockingPersons", personPayload);
        extraProperties.put("resolutionActions", List.of(
                "Reassign or unassign each listed asset (POST/DELETE /api/v1/assets/{assetId}/assignment[/department])",
                "Move each listed person to another department (PATCH /api/v1/persons/{id})"));

        return new ConflictException(
                "DEPARTMENT_HAS_DEPENDENTS",
                "Cannot delete department with dependent assets or persons",
                blockingAssets.size() + " asset(s) and " + blockingPersons.size() + " person(s) still reference "
                        + "department '" + department.getName() + "' and must be reassigned before it can be deleted.",
                extraProperties);
    }
}
