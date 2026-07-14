package com.iams.org.api;

import com.iams.org.api.dto.OrgLevelResponse;
import com.iams.org.api.dto.OrgNodeResponse;
import com.iams.org.domain.OrgLevel;
import com.iams.org.domain.OrgNode;
import org.springframework.stereotype.Component;

@Component
public class OrgHierarchyMapper {

    public OrgLevelResponse toResponse(OrgLevel level) {
        return new OrgLevelResponse(level.getId(), level.getVersion(), level.getCode(), level.getName(),
                level.getRank(), level.getRoomVariants());
    }

    public OrgNodeResponse toResponse(OrgNode node) {
        return new OrgNodeResponse(
                node.getId(),
                node.getName(),
                node.getCode(),
                node.isActive(),
                node.getParent() != null ? node.getParent().getId() : null,
                node.getParent() != null ? node.getParent().getName() : null,
                node.getLevel().getId(),
                node.getLevel().getName(),
                node.getLevel().getCode(),
                node.getPath(),
                node.getRoomVariant()
        );
    }
}
