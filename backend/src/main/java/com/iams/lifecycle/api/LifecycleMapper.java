package com.iams.lifecycle.api;

import com.iams.lifecycle.api.dto.ApprovalDelegationResponse;
import com.iams.lifecycle.api.dto.DisposalResponse;
import com.iams.lifecycle.api.dto.TransferResponse;
import com.iams.lifecycle.domain.ApprovalDelegation;
import com.iams.lifecycle.domain.AssetDisposalRequest;
import com.iams.lifecycle.domain.AssetTransferRequest;
import org.springframework.stereotype.Component;

@Component
public class LifecycleMapper {

    public TransferResponse toResponse(AssetTransferRequest request) {
        return new TransferResponse(
                request.getId(),
                request.getVersion(),
                request.getAsset().getId(),
                request.getAsset().getAssetNumber(),
                request.getFromOrgNode() != null ? request.getFromOrgNode().getId() : null,
                request.getFromOrgNode() != null ? request.getFromOrgNode().getCode() : null,
                request.getToOrgNode().getId(),
                request.getToOrgNode().getCode(),
                request.getFromPersonId(),
                request.getToPersonId(),
                request.getReason(),
                request.getStatus(),
                request.getNominalApproverId(),
                request.getEffectiveApproverId(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getDecidedBy(),
                request.getDecidedAt(),
                request.getRejectionReason()
        );
    }

    public DisposalResponse toResponse(AssetDisposalRequest request) {
        return new DisposalResponse(
                request.getId(),
                request.getVersion(),
                request.getAsset().getId(),
                request.getAsset().getAssetNumber(),
                request.getDisposalType(),
                request.getReason(),
                request.getStatus(),
                request.getNominalApproverId(),
                request.getEffectiveApproverId(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getDecidedBy(),
                request.getDecidedAt(),
                request.getRejectionReason(),
                request.getRestoredAt(),
                request.getRestoredBy()
        );
    }

    public ApprovalDelegationResponse toResponse(ApprovalDelegation delegation) {
        return new ApprovalDelegationResponse(
                delegation.getId(),
                delegation.getVersion(),
                delegation.getDelegatorUserId(),
                delegation.getDelegateUserId(),
                delegation.getValidFrom(),
                delegation.getValidTo(),
                delegation.isActive(),
                delegation.getReason()
        );
    }
}
