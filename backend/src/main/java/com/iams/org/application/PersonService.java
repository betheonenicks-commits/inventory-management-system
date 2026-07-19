package com.iams.org.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Department;
import com.iams.org.domain.DepartmentRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.org.domain.PersonType;
import com.iams.usr.application.OrgScopeGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal Person CRUD (FR-ORG-04) - just enough for asset assignment
 * (FR-LIF-04) to have someone to assign to. No delete: erasure is a later,
 * approval-gated CMP-epic workflow, not a plain DELETE here.
 * <p>
 * List/get/update are all org-scoped (FR-USR-04) via OrgScopeGuard - the
 * scope node itself or any descendant, since EPIC-ORG's hierarchy
 * (2026-07-13). update() checks scope against BOTH the person's current org node (via
 * get()) AND any new org node being written - a scoped actor can't use an
 * update to relocate a person into a node outside their own scope, even if
 * the person currently sits inside it. Filtering the unpaginated list()
 * result in memory (rather than a new repository query) matches this
 * service's existing scale: it was never paginated to begin with.
 */
@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public PersonService(PersonRepository personRepository, OrgNodeRepository orgNodeRepository,
                          DepartmentRepository departmentRepository, CurrentUserProvider currentUserProvider,
                          OrgScopeGuard scopeGuard) {
        this.personRepository = personRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.departmentRepository = departmentRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional(readOnly = true)
    public List<Person> list(String query) {
        List<Person> matches = (query == null || query.isBlank())
                ? personRepository.findAllByOrderByFullNameAsc()
                : personRepository.findByFullNameContainingIgnoreCaseOrderByFullNameAsc(query);
        return scopeGuard.filterToScope(matches, person -> person.getOrgNode() != null ? person.getOrgNode().getId() : null);
    }

    @Transactional(readOnly = true)
    public Person get(UUID id) {
        Person person = personRepository.findById(id).orElseThrow(() -> NotFoundException.of("Person", id));
        UUID orgNodeId = person.getOrgNode() != null ? person.getOrgNode().getId() : null;
        scopeGuard.requireWithinScope(orgNodeId, "person", id);
        return person;
    }

    @Transactional
    public Person create(String fullName, String email, PersonType personType, UUID orgNodeId, UUID departmentId) {
        if (fullName == null || fullName.isBlank()) {
            throw ValidationFailedException.singleField("fullName", "This field is required");
        }
        if (personType == null) {
            throw ValidationFailedException.singleField("personType", "This field is required");
        }

        Person person = new Person();
        person.setFullName(fullName);
        person.setEmail(email);
        person.setPersonType(personType);
        person.setOrgNode(resolveOrgNode(orgNodeId));
        person.setDepartmentId(resolveActiveDepartmentId(departmentId));
        person.setActive(true);
        person.setCreatedBy(currentUserProvider.current().id());

        return personRepository.save(person);
    }

    @Transactional
    public Person update(UUID id, String fullName, String email, PersonType personType, UUID orgNodeId,
                          UUID departmentId, Boolean active, long expectedVersion) {
        Person person = get(id); // enforces scope against the person's CURRENT org node
        if (person.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, person.getVersion(), person);
        }

        if (fullName != null) {
            if (fullName.isBlank()) {
                throw ValidationFailedException.singleField("fullName", "This field is required");
            }
            person.setFullName(fullName);
        }
        if (email != null) {
            person.setEmail(email);
        }
        if (personType != null) {
            person.setPersonType(personType);
        }
        if (orgNodeId != null) {
            // Also enforce scope against the NEW org node - otherwise a scoped actor could
            // use this to relocate a person into a node outside their own authority.
            scopeGuard.requireWithinScope(orgNodeId, "person", id);
            person.setOrgNode(resolveOrgNode(orgNodeId));
        }
        if (departmentId != null) {
            // null here means "leave unchanged", same convention as orgNode above.
            person.setDepartmentId(resolveActiveDepartmentId(departmentId));
        }
        if (active != null) {
            person.setActive(active);
        }
        person.setUpdatedBy(currentUserProvider.current().id());

        try {
            return personRepository.saveAndFlush(person);
        } catch (OptimisticLockingFailureException e) {
            Person current = get(id);
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }

    private OrgNode resolveOrgNode(UUID orgNodeId) {
        if (orgNodeId == null) {
            return null;
        }
        return orgNodeRepository.findById(orgNodeId).orElseThrow(() -> NotFoundException.of("OrgNode", orgNodeId));
    }

    /** A person may only be placed into a department that exists and is active. */
    private UUID resolveActiveDepartmentId(UUID departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> NotFoundException.of("Department", departmentId));
        if (!department.isActive()) {
            throw new ConflictException("DEPARTMENT_INACTIVE",
                    "Department '" + department.getName() + "' is not active and cannot be assigned to a person.");
        }
        return department.getId();
    }
}
