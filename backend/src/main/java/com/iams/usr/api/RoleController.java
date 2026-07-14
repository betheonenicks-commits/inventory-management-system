package com.iams.usr.api;

import com.iams.usr.api.dto.RoleCreateRequest;
import com.iams.usr.api.dto.RoleResponse;
import com.iams.usr.api.dto.RoleUpdateRequest;
import com.iams.usr.application.RoleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-USR-02: custom roles with a configurable permission set. Reading the
 * catalog (list/get) is available to any Administrator-or-above since
 * UserController's create form needs it to offer role choices; creating,
 * editing, and deleting a role is Super Administrator only (FR-USR-02's
 * primary actor).
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper mapper;

    public RoleController(RoleService roleService, RoleMapper mapper) {
        this.roleService = roleService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@perm.has('roles:read')")
    public List<RoleResponse> list() {
        return roleService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('roles:read')")
    public RoleResponse get(@PathVariable UUID id) {
        return mapper.toResponse(roleService.get(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('roles:write')")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        var role = roleService.createCustom(request.code(), request.name(), request.description(), request.permissions());
        RoleResponse response = mapper.toResponse(role);
        return ResponseEntity.created(URI.create("/api/v1/roles/" + role.getId())).body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.has('roles:write')")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleUpdateRequest request) {
        var role = roleService.updatePermissions(id, request.name(), request.description(), request.permissions(), request.version());
        return mapper.toResponse(role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('roles:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
