package com.iams.org.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.org.domain.PersonType;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.application.UserScopeResolver;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private OrgNodeRepository orgNodeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserScopeResolver scopeResolver;

    @Mock
    private SecurityEventLogger securityEventLogger;

    private PersonService service;

    @BeforeEach
    void setUp() {
        OrgScopeGuard scopeGuard = new OrgScopeGuard(currentUserProvider, scopeResolver, orgNodeRepository, securityEventLogger);
        service = new PersonService(personRepository, orgNodeRepository, departmentRepository, currentUserProvider, scopeGuard);
    }

    private Department activeDepartment() {
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName("Facilities");
        department.setCostCenterCode("CC-100");
        department.setActive(true);
        return department;
    }

    // get()/update() now resolve org scope (FR-USR-04) on every call, which requires
    // a CurrentUser to read the actor id from - stubCurrentUser() is a prerequisite
    // for those, not just for createdBy/updatedBy as before. scopeResolver is left
    // unstubbed deliberately: Mockito's default answer for an Optional-returning
    // method is Optional.empty(), which is exactly "unrestricted" here.
    private void stubCurrentUser() {
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "tester", Set.of("SUPER_ADMIN")));
    }

    /** Registers an OrgNode with a path, findable by OrgScopeGuard (path-prefix scope, since EPIC-ORG's hierarchy). */
    private OrgNode nodeAt(UUID id, String path) {
        OrgNode node = new OrgNode();
        node.setId(id);
        node.setPath(path);
        lenient().when(orgNodeRepository.findById(id)).thenReturn(Optional.of(node));
        return node;
    }

    @Test
    void create_succeeds_withoutOrgNode() {
        stubCurrentUser();
        when(personRepository.save(org.mockito.ArgumentMatchers.any(Person.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Person result = service.create("Alice Employee", "alice@example.org", PersonType.EMPLOYEE, null, null);

        assertThat(result.getFullName()).isEqualTo("Alice Employee");
        assertThat(result.getPersonType()).isEqualTo(PersonType.EMPLOYEE);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getOrgNode()).isNull();
        assertThat(result.getDepartmentId()).isNull();
    }

    @Test
    void create_resolvesActiveDepartment_whenProvided() {
        stubCurrentUser();
        Department department = activeDepartment();
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(personRepository.save(org.mockito.ArgumentMatchers.any(Person.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Person result = service.create("Dee Dept", null, PersonType.EMPLOYEE, null, department.getId());

        assertThat(result.getDepartmentId()).isEqualTo(department.getId());
    }

    @Test
    void create_rejectsInactiveDepartment() {
        // create() resolves the department before stamping createdBy, so no CurrentUser stub is needed.
        Department department = activeDepartment();
        department.setActive(false);
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> service.create("Ed", null, PersonType.EMPLOYEE, null, department.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsUnknownDepartment() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Fay", null, PersonType.EMPLOYEE, null, departmentId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_resolvesOrgNode_whenProvided() {
        stubCurrentUser();
        OrgNode node = new OrgNode();
        node.setId(UUID.randomUUID());
        node.setName("Building A");
        when(orgNodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        when(personRepository.save(org.mockito.ArgumentMatchers.any(Person.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Person result = service.create("Bob Volunteer", null, PersonType.VOLUNTEER, node.getId(), null);

        assertThat(result.getOrgNode()).isSameAs(node);
    }

    @Test
    void create_rejectsBlankFullName() {
        assertThatThrownBy(() -> service.create("  ", null, PersonType.EMPLOYEE, null, null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownOrgNode() {
        UUID orgNodeId = UUID.randomUUID();
        when(orgNodeRepository.findById(orgNodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Carol", null, PersonType.EMPLOYEE, orgNodeId, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_rejectsStaleVersion() {
        stubCurrentUser();
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setVersion(3L);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> service.update(person.getId(), "New Name", null, null, null, null, null, 2))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void update_appliesActiveFlag() {
        stubCurrentUser();
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setVersion(0L);
        person.setFullName("Dana");
        person.setActive(true);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(personRepository.saveAndFlush(person)).thenReturn(person);

        Person result = service.update(person.getId(), null, null, null, null, null, false, 0);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void update_setsDepartment_whenProvided() {
        stubCurrentUser();
        Department department = activeDepartment();
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setVersion(0L);
        person.setFullName("Gita");
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(personRepository.saveAndFlush(person)).thenReturn(person);

        Person result = service.update(person.getId(), null, null, null, null, department.getId(), null, 0);

        assertThat(result.getDepartmentId()).isEqualTo(department.getId());
    }

    @Test
    void get_blocked_whenPersonOutsideRequesterScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "deptHead", Set.of("DEPARTMENT_HEAD")));
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, "/" + scopeNodeId + "/");

        OrgNode otherNode = nodeAt(UUID.randomUUID(), "/" + UUID.randomUUID() + "/");
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setOrgNode(otherNode);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));

        assertThatThrownBy(() -> service.get(person.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void get_succeeds_whenPersonWithinRequesterScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "deptHead", Set.of("DEPARTMENT_HEAD")));
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));

        OrgNode node = nodeAt(scopeNodeId, "/" + scopeNodeId + "/");
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setOrgNode(node);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));

        assertThat(service.get(person.getId())).isSameAs(person);
    }

    @Test
    void list_filtersOutPeopleOutsideRequesterScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "deptHead", Set.of("DEPARTMENT_HEAD")));
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        String scopePath = "/" + scopeNodeId + "/";
        nodeAt(scopeNodeId, scopePath);

        OrgNode inScope = nodeAt(scopeNodeId, scopePath);
        OrgNode outOfScope = nodeAt(UUID.randomUUID(), "/" + UUID.randomUUID() + "/");

        Person inScopePerson = new Person();
        inScopePerson.setFullName("In Scope");
        inScopePerson.setOrgNode(inScope);
        Person outOfScopePerson = new Person();
        outOfScopePerson.setFullName("Out Of Scope");
        outOfScopePerson.setOrgNode(outOfScope);

        when(personRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(inScopePerson, outOfScopePerson));
        when(orgNodeRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<UUID> ids = invocation.getArgument(0);
            List<OrgNode> result = new java.util.ArrayList<>();
            for (UUID id : ids) {
                if (id.equals(inScope.getId())) {
                    result.add(inScope);
                } else if (id.equals(outOfScope.getId())) {
                    result.add(outOfScope);
                }
            }
            return result;
        });

        assertThat(service.list(null)).containsExactly(inScopePerson);
    }

    @Test
    void update_blocksRelocatingPersonToOutOfScopeOrgNode() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "deptHead", Set.of("DEPARTMENT_HEAD")));
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));

        OrgNode currentNode = nodeAt(scopeNodeId, "/" + scopeNodeId + "/"); // person currently WITHIN scope, so get()'s check passes
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setVersion(0L);
        person.setOrgNode(currentNode);
        when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));

        // target node OUTSIDE the actor's scope - deliberately unregistered with
        // orgNodeRepository, so its lookup resolves to empty and is correctly
        // treated as "not within scope" (conservative-by-default), same as a node
        // whose path simply doesn't match would be.
        UUID outOfScopeNodeId = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(person.getId(), null, null, null, outOfScopeNodeId, null, null, 0))
                .isInstanceOf(AccessDeniedException.class);
    }
}
