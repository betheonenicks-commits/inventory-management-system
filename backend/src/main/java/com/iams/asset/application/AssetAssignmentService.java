package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUserProvider;
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
 */
@Service
public class AssetAssignmentService {

    private final AssetRepository assetRepository;
    private final PersonRepository personRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;

    public AssetAssignmentService(AssetRepository assetRepository,
                                   PersonRepository personRepository,
                                   AssetHistoryRecorder historyRecorder,
                                   CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.personRepository = personRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public Asset assign(UUID assetId, UUID personId, long expectedVersion) {
        Asset asset = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));

        if (asset.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, asset.getVersion(), asset);
        }

        Person person = personRepository.findById(personId).orElseThrow(() -> NotFoundException.of("Person", personId));
        if (!person.isActive()) {
            throw new ConflictException("PERSON_INACTIVE", "Person '" + person.getFullName() + "' is not active and cannot be assigned an asset.");
        }

        String previousHolder = previousHolderName(asset);
        if (person.getId().equals(asset.getAssignedToPersonId())) {
            return asset; // no-op reassignment to the same person, nothing to record
        }

        asset.setAssignedToPersonId(person.getId());
        asset.setUpdatedBy(currentUserProvider.current().id());

        try {
            asset = assetRepository.saveAndFlush(asset);
        } catch (OptimisticLockingFailureException e) {
            Asset current = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }

        historyRecorder.record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "assignedToPersonId", previousHolder, person.getFullName());
        return asset;
    }

    @Transactional
    public Asset unassign(UUID assetId, long expectedVersion) {
        Asset asset = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));

        if (asset.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, asset.getVersion(), asset);
        }
        if (asset.getAssignedToPersonId() == null) {
            return asset; // already unassigned, nothing to record
        }

        String previousHolder = previousHolderName(asset);
        asset.setAssignedToPersonId(null);
        asset.setUpdatedBy(currentUserProvider.current().id());

        try {
            asset = assetRepository.saveAndFlush(asset);
        } catch (OptimisticLockingFailureException e) {
            Asset current = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }

        historyRecorder.record(asset, AssetHistoryEventType.ASSIGNMENT_CHANGE, "assignedToPersonId", previousHolder, null);
        return asset;
    }

    private String previousHolderName(Asset asset) {
        if (asset.getAssignedToPersonId() == null) {
            return null;
        }
        return personRepository.findById(asset.getAssignedToPersonId()).map(Person::getFullName).orElse(null);
    }
}
