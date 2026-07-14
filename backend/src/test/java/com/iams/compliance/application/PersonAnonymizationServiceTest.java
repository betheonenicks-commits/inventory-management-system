package com.iams.compliance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.org.domain.PersonType;
import com.iams.sec.application.SecurityEventLogger;
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
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;

    private PersonAnonymizationService service;
    private Person person;

    @BeforeEach
    void setUp() {
        service = new PersonAnonymizationService(personRepository, assetRepository, currentUserProvider, securityEventLogger);
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
}
