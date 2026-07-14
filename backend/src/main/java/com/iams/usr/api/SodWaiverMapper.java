package com.iams.usr.api;

import com.iams.usr.api.dto.SodWaiverResponse;
import com.iams.usr.domain.SodWaiver;
import org.springframework.stereotype.Component;

@Component
public class SodWaiverMapper {

    public SodWaiverResponse toResponse(SodWaiver waiver) {
        return new SodWaiverResponse(
                waiver.getId(),
                waiver.getVersion(),
                waiver.getScope(),
                waiver.getSignedOffBy().getId(),
                waiver.getSignedOffBy().getUsername(),
                waiver.getReason(),
                waiver.isActive(),
                waiver.getCreatedBy(),
                waiver.getCreatedAt()
        );
    }
}
