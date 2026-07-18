package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUserProvider;
import org.springframework.context.ApplicationEventPublisher;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custodian assignment (FR-LIF-04, US-AST-07 custodian tracking). Kept
 * separate from the general PATCH /assets/{id} for the same reason status
 * transitions are: assignment must always produce an ASSIGNMENT_CHANGE
 * history row, never get silently folded into a FIELD_UPDATE. Reassigning
 * captures old->new in that one row - that IS the "prior assignment
 * explicitly closed" record; no separate assignment-log entity needed.
 * <p>
 * US-LIF-04: the custodian is a Person XOR a Department. This service is the
 * single writer of both columns and keeps them mutually exclusive - assigning
 * either kind clears the other, so a person->department (or the reverse) switch
 * is one ASSIGNMENT_CHANGE row whose old/new custodian labels cross the kind
 * boundary. History is recorded under a unified "custodian" field name rather
 * than the raw column, because that reads correctly whichever kind is involved.
 */
@Service
public class AssetAssignmentService {

    private static final String CUSTODIAN_FIELD = "custodian";

    private final AssetRepository assetRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AssetAssignmentService(AssetRepository assetRepository,
                                   PersonRepository personRepository,
                                   DepartmentRepository departmentRepository,
                                   AssetHistoryRecorder historyRecorder,
                                   CurrentUserProvider currentUserProvider,
                                   ApplicationEventPublisher eventPublisher) {
        this.assetRepository = assetRepository;
        this.personRepository = personRepository;
        this.departmentRepository = departmentRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Asset assign(UUID assetId, UUID personId, long expectedVersion) {
        Asset asset = loadForUpdate(assetId, expectedVersion);

        Person person = personRepository.findById(personId).orElseThrow(() -> NotFoundException.of("Person", personId));
        if (!person.isActive()) {
            throw new ConflictException("PERSON_INACTIVE", "Person '" + person.getFullName() + "' is not active and cannot be assigned an asset.");
        }

        String previousCustodian = currentCustodianLabel(asset);
        if (person.getId().equals(asset.getAssignedToPersonId()) && asset.getAssignedToDepartmentId() == null) {
            return asset; // no-op reassignment to the same person, nothing to record
        }

        asset.setAssignedToPersonId(person.getId());
        asset.setAssignedToDepartmentId(null); // custodian is exclusive
        asset.setUpdatedBy(currentUserProvider.current().id());
        asset = save(asset, assetId, expectedVersion);

        historyRecorder.record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, CUSTODIAN_FIELD, previousCustodian, person.getFullName());
        // US-NTF-04: same transaction, so the notification commits with the assignment.
        eventPublisher.publishEvent(new AssetAssignmentChangedEvent(asset.getId(), asset.getAssetNumber(),
                asset.getName(), person.getId(), person.getFullName(), "assigned to",
                currentUserProvider.current().username()));
        return asset;
    }

    /**
     * US-LIF-04: assign the asset to a Department. There is deliberately no
     * AssetAssignmentChangedEvent here: US-NTF-04 notifies the assigned person,
     * and a department is not a notifiable recipient - there is no person to
     * notify. The custodianship change is still fully recorded in history.
     */
    @Transactional
    public Asset assignToDepartment(UUID assetId, UUID departmentId, long expectedVersion) {
        Asset asset = loadForUpdate(assetId, expectedVersion);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> NotFoundException.of("Department", departmentId));
        if (!department.isActive()) {
            throw new ConflictException("DEPARTMENT_INACTIVE",
                    "Department '" + department.getName() + "' is not active and cannot be assigned an asset.");
        }

        String previousCustodian = currentCustodianLabel(asset);
        if (department.getId().equals(asset.getAssignedToDepartmentId()) && asset.getAssignedToPersonId() == null) {
            return asset; // no-op reassignment to the same department
        }

        asset.setAssignedToDepartmentId(department.getId());
        asset.setAssignedToPersonId(null); // custodian is exclusive
        asset.setUpdatedBy(currentUserProvider.current().id());
        asset = save(asset, assetId, expectedVersion);

        historyRecorder.record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, CUSTODIAN_FIELD,
                previousCustodian, departmentLabel(department));
        return asset;
    }

    @Transactional
    public Asset unassign(UUID assetId, long expectedVersion) {
        Asset asset = loadForUpdate(assetId, expectedVersion);

        if (asset.getAssignedToPersonId() == null && asset.getAssignedToDepartmentId() == null) {
            return asset; // already unassigned, nothing to record
        }

        String previousCustodian = currentCustodianLabel(asset);
        UUID previousPersonId = asset.getAssignedToPersonId();
        asset.setAssignedToPersonId(null);
        asset.setAssignedToDepartmentId(null);
        asset.setUpdatedBy(currentUserProvider.current().id());
        asset = save(asset, assetId, expectedVersion);

        historyRecorder.record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, CUSTODIAN_FIELD, previousCustodian, null);
        // Only a person can be notified of unassignment; a department cannot.
        if (previousPersonId != null) {
            eventPublisher.publishEvent(new AssetAssignmentChangedEvent(asset.getId(), asset.getAssetNumber(),
                    asset.getName(), previousPersonId, previousCustodian, "unassigned",
                    currentUserProvider.current().username()));
        }
        return asset;
    }

    private Asset loadForUpdate(UUID assetId, long expectedVersion) {
        Asset asset = assetRepository.findByIdWithAssociations(assetId)
                .orElseThrow(() -> NotFoundException.of("Asset", assetId));
        if (asset.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, asset.getVersion(), asset);
        }
        return asset;
    }

    private Asset save(Asset asset, UUID assetId, long expectedVersion) {
        try {
            return assetRepository.saveAndFlush(asset);
        } catch (OptimisticLockingFailureException e) {
            Asset current = assetRepository.findByIdWithAssociations(assetId)
                    .orElseThrow(() -> NotFoundException.of("Asset", assetId));
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }

    /** The human-readable custodian currently on the asset, or null when unassigned. */
    private String currentCustodianLabel(Asset asset) {
        if (asset.getAssignedToPersonId() != null) {
            return personRepository.findById(asset.getAssignedToPersonId()).map(Person::getFullName).orElse(null);
        }
        if (asset.getAssignedToDepartmentId() != null) {
            return departmentRepository.findById(asset.getAssignedToDepartmentId())
                    .map(AssetAssignmentService::departmentLabel).orElse(null);
        }
        return null;
    }

    private static String departmentLabel(Department department) {
        return department.getName() + " (" + department.getCostCenterCode() + ")";
    }
}
