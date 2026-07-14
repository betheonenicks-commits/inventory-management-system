package com.iams.sec.api;

import com.iams.common.web.PageResponse;
import com.iams.sec.api.dto.SecurityEventResponse;
import com.iams.sec.application.SecurityEventLogService;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** US-SEC-11: search and filter the Security & Access Log. */
@RestController
@RequestMapping("/api/v1/security-events")
public class SecurityEventController {

    private final SecurityEventLogService service;
    private final SecurityEventMapper mapper;

    public SecurityEventController(SecurityEventLogService service, SecurityEventMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@perm.has('security:read')")
    public PageResponse<SecurityEventResponse> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) SecurityEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        var page = service.search(userId, eventType, from, to, pageable);
        List<SecurityEventResponse> data = page.getContent().stream().map(mapper::toResponse).collect(Collectors.toList());
        return new PageResponse<>(data,
                new PageResponse.PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()),
                List.of("createdAt,DESC"));
    }
}
