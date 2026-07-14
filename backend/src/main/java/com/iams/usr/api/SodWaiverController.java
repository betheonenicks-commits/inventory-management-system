package com.iams.usr.api;

import com.iams.usr.api.dto.SodWaiverCreateRequest;
import com.iams.usr.api.dto.SodWaiverResponse;
import com.iams.usr.application.SodWaiverService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-USR-09: record and view Separation-of-Duties waivers. */
@RestController
@RequestMapping("/api/v1/sod-waivers")
public class SodWaiverController {

    private final SodWaiverService waiverService;
    private final SodWaiverMapper mapper;

    public SodWaiverController(SodWaiverService waiverService, SodWaiverMapper mapper) {
        this.waiverService = waiverService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@perm.has('sod-waivers:write')")
    public List<SodWaiverResponse> list() {
        return waiverService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('sod-waivers:write')")
    public SodWaiverResponse get(@PathVariable UUID id) {
        return mapper.toResponse(waiverService.get(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('sod-waivers:write')")
    public ResponseEntity<SodWaiverResponse> create(@Valid @RequestBody SodWaiverCreateRequest request) {
        var waiver = waiverService.create(request.scope(), request.signedOffByUserId(), request.reason());
        SodWaiverResponse response = mapper.toResponse(waiver);
        return ResponseEntity.created(URI.create("/api/v1/sod-waivers/" + waiver.getId())).body(response);
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("@perm.has('sod-waivers:write')")
    public SodWaiverResponse revoke(@PathVariable UUID id) {
        return mapper.toResponse(waiverService.revoke(id));
    }
}
