package com.iams.org.api;

import com.iams.org.api.dto.PersonResponse;
import com.iams.org.domain.Person;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

    public PersonResponse toResponse(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getVersion(),
                person.getFullName(),
                person.getEmail(),
                person.getPersonType(),
                person.getOrgNode() != null ? person.getOrgNode().getId() : null,
                person.getOrgNode() != null ? person.getOrgNode().getName() : null,
                person.isActive(),
                person.getCreatedBy(),
                person.getCreatedAt(),
                person.getUpdatedBy(),
                person.getUpdatedAt()
        );
    }
}
