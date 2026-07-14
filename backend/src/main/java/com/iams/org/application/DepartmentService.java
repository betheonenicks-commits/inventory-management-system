package com.iams.org.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-ORG-03: departments/cost centers as their own dimension, independent of
 * the physical org_node hierarchy.
 * <p>
 * Known limitation, not silently papered over: {@link #delete} has no
 * dependent-asset/dependent-person check yet, because neither Asset nor
 * Person references a Department yet (see DEVELOPMENT_LOG.md 2026-07-13) -
 * there is nothing to block on today. AC-ORG-03-X ("blocked while assets are
 * assigned") only becomes enforceable once that FK exists.
 */
@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public DepartmentService(DepartmentRepository departmentRepository, CurrentUserProvider currentUserProvider) {
        this.departmentRepository = departmentRepository;
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
        departmentRepository.delete(department);
    }
}
