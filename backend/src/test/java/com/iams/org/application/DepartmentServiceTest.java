package com.iams.org.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private DepartmentService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(departmentRepository, assetRepository, currentUserProvider);
        lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "admin", Set.of("ADMIN")));
    }

    @Test
    void create_succeeds() {
        when(departmentRepository.existsByCostCenterCode("CC-100")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        Department result = service.create("Science Department", "CC-100");

        assertThat(result.getName()).isEqualTo("Science Department");
        assertThat(result.getCostCenterCode()).isEqualTo("CC-100");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void create_rejectsDuplicateCostCenterCode() {
        when(departmentRepository.existsByCostCenterCode("CC-100")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Science Department", "CC-100"))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void update_rejectsStaleVersion() {
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setVersion(2L);
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> service.update(department.getId(), "New Name", null, null, 1L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void get_rejectsUnknownDepartment() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_succeeds_whenNoAssetsAssigned() {
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName("Facilities");
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(assetRepository.findByAssignedToDepartmentId(department.getId())).thenReturn(List.of());

        service.delete(department.getId());

        verify(departmentRepository).delete(department);
    }

    @Test
    void delete_blockedWithDependentList_whenAssetsAreAssigned() {
        // AC-ORG-03-X: blocked with the dependent list, not a raw FK-violation 500.
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName("Facilities");
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber("AST-2026-000042");
        asset.setName("Forklift");
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(assetRepository.findByAssignedToDepartmentId(department.getId())).thenReturn(List.of(asset));

        assertThatThrownBy(() -> service.delete(department.getId()))
                .isInstanceOfSatisfying(ConflictException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("DEPARTMENT_HAS_ASSIGNED_ASSETS");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> blocking =
                            (List<Map<String, Object>>) ex.getExtraProperties().get("blockingAssets");
                    assertThat(blocking).hasSize(1);
                    assertThat(blocking.get(0)).containsEntry("assetNumber", "AST-2026-000042");
                    assertThat(ex.getExtraProperties()).containsKey("resolutionActions");
                });

        verify(departmentRepository, never()).delete(any());
    }
}
