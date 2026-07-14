package com.iams.audit.application;

import com.iams.asset.domain.Asset;
import com.iams.audit.domain.AuditExpectedAsset;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-23's automatic trigger: EPIC-LIF's transfer/disposal services call
 * {@link #flagIfInActiveAudit} whenever an approved transfer or disposal
 * actually moves/retires an asset - this is the "asset transferred/disposed"
 * event that a prior EPIC-AUD session's own RTM entry noted didn't exist
 * anywhere yet.
 * <p>
 * Deliberately narrower than the full AC: it can only flag an asset that
 * hasn't been scanned in the audit yet (no {@link AuditFinding} row exists
 * for that audit/asset pair). If the asset was already scanned and verified
 * before the move, the existing finding is immutable (US-AUD-24) and is left
 * standing rather than retroactively reclassified - documented as a real,
 * narrower-than-ideal limitation, not silently glossed over.
 */
@Service
public class AuditScopeChangeService {

    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditFindingRepository findingRepository;

    public AuditScopeChangeService(AuditExpectedAssetRepository expectedAssetRepository, AuditFindingRepository findingRepository) {
        this.expectedAssetRepository = expectedAssetRepository;
        this.findingRepository = findingRepository;
    }

    @Transactional
    public void flagIfInActiveAudit(Asset asset) {
        for (AuditExpectedAsset row : expectedAssetRepository.findByAssetIdAndAuditStatus(asset.getId(), AuditStatus.IN_PROGRESS)) {
            if (findingRepository.findByAuditIdAndAssetId(row.getAudit().getId(), asset.getId()).isPresent()) {
                continue;
            }
            AuditFinding finding = new AuditFinding();
            finding.setAudit(row.getAudit());
            finding.setAsset(asset);
            finding.setStatus(FindingStatus.SCOPE_CHANGED);
            finding.setVerifiedAt(Instant.now());
            findingRepository.save(finding);
        }
    }
}
