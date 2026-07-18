package com.iams.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.org.domain.PersonType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetAssignmentServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AssetHistoryRecorder historyRecorder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private AssetAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssetAssignmentService(assetRepository, personRepository, departmentRepository, historyRecorder,
                currentUserProvider, eventPublisher);
    }

    private Department department(String name, String code, boolean active) {
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName(name);
        department.setCostCenterCode(code);
        department.setActive(active);
        return department;
    }

    private Asset asset(long version) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber("AST-2026-000001");
        asset.setVersion(version);
        return asset;
    }

    private Person person(String name, boolean active) {
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setFullName(name);
        person.setPersonType(PersonType.EMPLOYEE);
        person.setActive(active);
        return person;
    }

    private void stubCurrentUser() {
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "tester", Set.of("SUPER_ADMIN")));
    }

    @Test
    void assign_succeeds_andRecordsHistory() {
        Asset asset = asset(0);
        Person alice = person("Alice", true);
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(personRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        Asset result = service.assign(asset.getId(), alice.getId(), 0);

        assertThat(result.getAssignedToPersonId()).isEqualTo(alice.getId());
        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian", null, "Alice");
    }

    @Test
    void reassign_capturesOldAndNewHolderInOneHistoryRow() {
        Asset asset = asset(0);
        Person bob = person("Bob", true);
        Person carol = person("Carol", true);
        asset.setAssignedToPersonId(bob.getId());
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(personRepository.findById(carol.getId())).thenReturn(Optional.of(carol));
        when(personRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        service.assign(asset.getId(), carol.getId(), 0);

        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian", "Bob", "Carol");
    }

    @Test
    void assign_rejectsInactivePerson() {
        Asset asset = asset(0);
        Person inactive = person("Dana", false);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(personRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.assign(asset.getId(), inactive.getId(), 0))
                .isInstanceOf(ConflictException.class);

        verify(historyRecorder, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void assign_rejectsStaleVersion() {
        Asset asset = asset(3);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.assign(asset.getId(), UUID.randomUUID(), 2))
                .isInstanceOf(OptimisticLockConflictException.class);

        verify(personRepository, never()).findById(any());
    }

    @Test
    void assign_rejectsWhenPersonNotFound() {
        Asset asset = asset(0);
        UUID personId = UUID.randomUUID();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(asset.getId(), personId, 0))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unassign_succeeds_andRecordsHistory() {
        Asset asset = asset(0);
        Person alice = person("Alice", true);
        asset.setAssignedToPersonId(alice.getId());
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(personRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        Asset result = service.unassign(asset.getId(), 0);

        assertThat(result.getAssignedToPersonId()).isNull();
        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian", "Alice", null);
    }

    @Test
    void unassign_isNoOpWhenAlreadyUnassigned() {
        Asset asset = asset(0);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        service.unassign(asset.getId(), 0);

        verify(historyRecorder, never()).record(any(), any(), any(), any(), any());
        verify(assetRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignToDepartment_succeeds_recordsHistory_andDoesNotNotify() {
        Asset asset = asset(0);
        Department facilities = department("Facilities", "CC-100", true);
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(departmentRepository.findById(facilities.getId())).thenReturn(Optional.of(facilities));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        Asset result = service.assignToDepartment(asset.getId(), facilities.getId(), 0);

        assertThat(result.getAssignedToDepartmentId()).isEqualTo(facilities.getId());
        assertThat(result.getAssignedToPersonId()).isNull();
        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian", null,
                "Facilities (CC-100)");
        // A department is not a notifiable recipient - no assignment event is published.
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void assignToDepartment_rejectsInactiveDepartment() {
        Asset asset = asset(0);
        Department retired = department("Retired Dept", "CC-999", false);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(departmentRepository.findById(retired.getId())).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> service.assignToDepartment(asset.getId(), retired.getId(), 0))
                .isInstanceOf(ConflictException.class);

        verify(historyRecorder, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void assignPerson_toAnAssetHeldByADepartment_closesTheDepartmentInOneRow() {
        Asset asset = asset(0);
        Department facilities = department("Facilities", "CC-100", true);
        asset.setAssignedToDepartmentId(facilities.getId());
        Person erin = person("Erin", true);
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(departmentRepository.findById(facilities.getId())).thenReturn(Optional.of(facilities));
        when(personRepository.findById(erin.getId())).thenReturn(Optional.of(erin));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        service.assign(asset.getId(), erin.getId(), 0);

        // The prior department custodian is explicitly closed - old=department, new=person - and the column cleared.
        assertThat(asset.getAssignedToDepartmentId()).isNull();
        assertThat(asset.getAssignedToPersonId()).isEqualTo(erin.getId());
        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian",
                "Facilities (CC-100)", "Erin");
    }

    @Test
    void assignToDepartment_forAnAssetHeldByAPerson_closesThePersonInOneRow() {
        Asset asset = asset(0);
        Person frank = person("Frank", true);
        asset.setAssignedToPersonId(frank.getId());
        Department ops = department("Operations", "CC-200", true);
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(personRepository.findById(frank.getId())).thenReturn(Optional.of(frank));
        when(departmentRepository.findById(ops.getId())).thenReturn(Optional.of(ops));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        service.assignToDepartment(asset.getId(), ops.getId(), 0);

        assertThat(asset.getAssignedToPersonId()).isNull();
        assertThat(asset.getAssignedToDepartmentId()).isEqualTo(ops.getId());
        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian", "Frank",
                "Operations (CC-200)");
    }

    @Test
    void unassign_fromADepartmentCustodian_recordsHistoryButPublishesNoEvent() {
        Asset asset = asset(0);
        Department facilities = department("Facilities", "CC-100", true);
        asset.setAssignedToDepartmentId(facilities.getId());
        stubCurrentUser();
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(departmentRepository.findById(facilities.getId())).thenReturn(Optional.of(facilities));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);

        service.unassign(asset.getId(), 0);

        assertThat(asset.getAssignedToDepartmentId()).isNull();
        verify(historyRecorder).record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "custodian",
                "Facilities (CC-100)", null);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
