package com.iams.compliance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.LegalHoldScopeType;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.org.domain.PersonType;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonAnonymizationServiceTest {

    @Mock private PersonRepository personRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private LegalHoldService legalHoldService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;

    private PersonAnonymizationService service;
    private Person person;

    @BeforeEach
    void setUp() {
        service = new PersonAnonymizationService(personRepository, assetRepository, appUserRepository, auditRepository,
                legalHoldService, currentUserProvider, securityEventLogger);
        person = new Person();
        person.setId(UUID.randomUUID());
        person.setFullName("Jordan Casey");
        person.setEmail("jordan@example.com");
        person.setPersonType(PersonType.EMPLOYEE);
        person.setActive(false);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "compliance1", Set.of("COMPLIANCE_OFFICER")));
    }

    @Test
    void eligible_excludesActivePersons_andAlreadyAnonymizedPersons() {
        Person active = new Person();
        active.setActive(true);
        Person alreadyAnonymized = new Person();
        alreadyAnonymized.setActive(false);
        alreadyAnonymized.setAnonymizedAt(Instant.now());
        when(personRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(person, active, alreadyAnonymized));

        List<Person> result = service.eligible();

        assertThat(result).containsExactly(person);
    }

    @Test
    void anonymize_rejectsUnknownPerson() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anonymize(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void anonymize_rejectsStillActivePerson() {
        person.setActive(true);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> service.anonymize(person.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void anonymize_rejectsAlreadyAnonymizedPerson() {
        person.setAnonymizedAt(Instant.now());
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> service.anonymize(person.getId())).isInstanceOf(ConflictException.class);
    }

    /** US-LIF-14: the exact story this closes - erasure blocked while assets remain assigned. */
    @Test
    void anonymize_blocksWhileAssetsStillAssigned() {
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        Asset assignedAsset = new Asset();
        assignedAsset.setId(UUID.randomUUID());
        assignedAsset.setAssetNumber("AST-2026-000099");
        assignedAsset.setName("Laptop");
        when(assetRepository.findByAssignedToPersonId(person.getId())).thenReturn(List.of(assignedAsset));

        assertThatThrownBy(() -> service.anonymize(person.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("assigned");
    }

    @Test
    void anonymize_succeeds_preservingIdAsStablePseudonym() {
        UUID personId = person.getId();
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of());
        when(personRepository.saveAndFlush(person)).thenReturn(person);

        Person result = service.anonymize(personId);

        assertThat(result.getId()).isEqualTo(personId);
        assertThat(result.getFullName()).isEqualTo("Anonymized Person");
        assertThat(result.getEmail()).isNull();
        assertThat(result.getAnonymizedAt()).isNotNull();
    }

    // AC-SEC-10-X: "an active legal hold on a linked audit" blocks anonymization (423).
    @Test
    void anonymize_succeeds_whenPersonHasNoLinkedLoginAccount() {
        // The common case: a departed person who never had a login - appUserRepository.findByPersonId
        // defaults to Optional.empty() unstubbed, so the whole linked-audit check is a no-op.
        UUID personId = person.getId();
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of());
        when(personRepository.saveAndFlush(person)).thenReturn(person);

        Person result = service.anonymize(personId);

        assertThat(result.getAnonymizedAt()).isNotNull();
        org.mockito.Mockito.verifyNoInteractions(auditRepository);
    }

    @Test
    void anonymize_refusesWhenALinkedAuditHasAnActiveLegalHold() {
        UUID personId = person.getId();
        UUID linkedUserId = UUID.randomUUID();
        UUID linkedAuditId = UUID.randomUUID();
        AppUser linkedUser = new AppUser();
        linkedUser.setId(linkedUserId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of());
        when(appUserRepository.findByPersonId(personId)).thenReturn(Optional.of(linkedUser));
        when(auditRepository.findAuditIdsLinkedToUser(linkedUserId)).thenReturn(List.of(linkedAuditId));
        org.mockito.Mockito.doThrow(new LegalHoldActiveException(LegalHoldScopeType.AUDIT))
                .when(legalHoldService).requireNoActiveHold(LegalHoldScopeType.AUDIT, linkedAuditId);

        assertThatThrownBy(() -> service.anonymize(personId)).isInstanceOf(LegalHoldActiveException.class);
        org.mockito.Mockito.verify(personRepository, org.mockito.Mockito.never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anonymize_succeeds_whenLinkedAuditsHaveNoActiveHold() {
        UUID personId = person.getId();
        UUID linkedUserId = UUID.randomUUID();
        AppUser linkedUser = new AppUser();
        linkedUser.setId(linkedUserId);
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of());
        when(appUserRepository.findByPersonId(personId)).thenReturn(Optional.of(linkedUser));
        when(auditRepository.findAuditIdsLinkedToUser(linkedUserId)).thenReturn(List.of(UUID.randomUUID()));
        when(personRepository.saveAndFlush(person)).thenReturn(person);
        // legalHoldService.requireNoActiveHold is a no-op mock (no active hold) - anonymization proceeds.

        Person result = service.anonymize(personId);

        assertThat(result.getAnonymizedAt()).isNotNull();
    }

    @Test
    void exportData_returnsPersonFieldsAndCurrentlyAssignedAssets() {
        UUID personId = person.getId();
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber("AST-2026-000042");
        asset.setName("Projector");
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of(asset));

        com.iams.compliance.api.dto.PersonDataExportResponse export = service.exportData(personId);

        assertThat(export.id()).isEqualTo(personId);
        assertThat(export.fullName()).isEqualTo("Jordan Casey");
        assertThat(export.email()).isEqualTo("jordan@example.com");
        assertThat(export.currentlyAssignedAssets()).hasSize(1);
        assertThat(export.currentlyAssignedAssets().get(0).assetNumber()).isEqualTo("AST-2026-000042");
        assertThat(export.exportedAt()).isNotNull();
    }

    @Test
    void exportData_throwsNotFound_whenPersonDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(personRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportData(missingId)).isInstanceOf(NotFoundException.class);
    }
}
