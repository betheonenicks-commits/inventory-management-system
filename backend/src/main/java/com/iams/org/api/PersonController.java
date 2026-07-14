package com.iams.org.api;

import com.iams.org.api.dto.PersonCreateRequest;
import com.iams.org.api.dto.PersonResponse;
import com.iams.org.api.dto.PersonUpdateRequest;
import com.iams.org.application.PersonService;
import com.iams.org.domain.Person;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal Person CRUD (FR-ORG-04) - exists so asset assignment (FR-LIF-04)
 * has someone to assign to. Not the full EPIC-ORG Person model.
 */
@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {

    private final PersonService personService;
    private final PersonMapper mapper;

    public PersonController(PersonService personService, PersonMapper mapper) {
        this.personService = personService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<PersonResponse> list(@RequestParam(required = false) String q) {
        return personService.list(q).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PersonResponse get(@PathVariable UUID id) {
        return mapper.toResponse(personService.get(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('org:write')")
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonCreateRequest request) {
        Person person = personService.create(request.fullName(), request.email(), request.personType(), request.orgNodeId());
        PersonResponse response = mapper.toResponse(person);
        return ResponseEntity.created(URI.create("/api/v1/persons/" + person.getId())).body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.has('org:write')")
    public PersonResponse update(@PathVariable UUID id, @Valid @RequestBody PersonUpdateRequest request) {
        Person person = personService.update(id, request.fullName(), request.email(), request.personType(),
                request.orgNodeId(), request.active(), request.version());
        return mapper.toResponse(person);
    }
}
