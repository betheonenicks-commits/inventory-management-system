package com.iams.org.api;

import com.iams.org.api.dto.DepartmentCreateRequest;
import com.iams.org.api.dto.DepartmentResponse;
import com.iams.org.api.dto.DepartmentUpdateRequest;
import com.iams.org.application.DepartmentService;
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

/** FR-ORG-03: departments/cost centers as their own dimension. Same access shape as PersonController/OrgHierarchyController. */
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final DepartmentMapper mapper;

    public DepartmentController(DepartmentService departmentService, DepartmentMapper mapper) {
        this.departmentService = departmentService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DepartmentResponse> list() {
        return departmentService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable UUID id) {
        return mapper.toResponse(departmentService.get(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('org:write')")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest request) {
        var department = departmentService.create(request.name(), request.costCenterCode());
        DepartmentResponse response = mapper.toResponse(department);
        return ResponseEntity.created(URI.create("/api/v1/departments/" + department.getId())).body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.has('org:write')")
    public DepartmentResponse update(@PathVariable UUID id, @Valid @RequestBody DepartmentUpdateRequest request) {
        var department = departmentService.update(id, request.name(), request.costCenterCode(), request.active(), request.version());
        return mapper.toResponse(department);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('org:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
