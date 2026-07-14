package com.iams.org.api;

import com.iams.org.api.dto.DepartmentResponse;
import com.iams.org.domain.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getVersion(),
                department.getName(),
                department.getCostCenterCode(),
                department.isActive(),
                department.getCreatedBy(),
                department.getCreatedAt(),
                department.getUpdatedBy(),
                department.getUpdatedAt()
        );
    }
}
