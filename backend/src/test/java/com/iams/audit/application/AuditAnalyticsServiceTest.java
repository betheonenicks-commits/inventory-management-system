package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFindingReconciliationRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditAnalyticsServiceTest {

    @Mock private AuditService auditService;
    @Mock private AuditExpectedAssetRepository expectedAssetRepository;
    @Mock private AuditFindingRepository findingRepository;
    @Mock private AuditFindingReconciliationRepository reconciliationRepository;

    private AuditAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AuditAnalyticsService(auditService, expectedAssetRepository, findingRepository, reconciliationRepository);
    }

    private Audit closedAudit(String name, String created, String approved) {
        Audit a = new Audit();
        a.setId(UUID.randomUUID());
        a.setName(name);
        a.setCreatedAt(Instant.parse(created));
        a.setApprovedAt(Instant.parse(approved));
        return a;
    }

    private void stubCounts(Audit a, long expected, long missing, long reconciled) {
        lenient().when(expectedAssetRepository.countByAuditId(a.getId())).thenReturn(expected);
        lenient().when(findingRepository.countByAuditIdAndStatus(a.getId(), FindingStatus.MISSING)).thenReturn(missing);
        lenient().when(reconciliationRepository.countByAuditId(a.getId())).thenReturn(reconciled);
    }

    @Test
    void crossCycleTrends_computesPerCycleMetrics_orderedChronologically() {
        // list(CLOSED) returns newest-first; the service must re-order oldest-first for a trend.
        Audit older = closedAudit("Q1 sweep", "2026-01-01T00:00:00Z", "2026-01-03T00:00:00Z"); // 2.0 days
        Audit newer = closedAudit("Q2 sweep", "2026-02-01T00:00:00Z", "2026-02-02T12:00:00Z"); // 1.5 days
        stubCounts(older, 100, 10, 0);   // 10% missing, none reconciled
        stubCounts(newer, 100, 5, 2);    // 5% raw missing, 2 reconciled -> 3% net
        when(auditService.list(AuditStatus.CLOSED)).thenReturn(List.of(newer, older));

        List<AuditAnalyticsService.AuditCycleTrend> trend = service.crossCycleTrends();

        assertThat(trend).extracting(AuditAnalyticsService.AuditCycleTrend::name)
                .containsExactly("Q1 sweep", "Q2 sweep"); // chronological
        AuditAnalyticsService.AuditCycleTrend q1 = trend.get(0);
        AuditAnalyticsService.AuditCycleTrend q2 = trend.get(1);
        assertThat(q1.missingRatePct()).isEqualTo(10.0);
        assertThat(q1.netMissingRatePct()).isEqualTo(10.0);
        assertThat(q1.completionDays()).isEqualTo(2.0);
        // AC-AUD-18: the reduction credits ONLY formal reconciliations.
        assertThat(q2.missingCount()).isEqualTo(5);
        assertThat(q2.reconciledCount()).isEqualTo(2);
        assertThat(q2.netMissingCount()).isEqualTo(3);
        assertThat(q2.missingRatePct()).isEqualTo(5.0);
        assertThat(q2.netMissingRatePct()).isEqualTo(3.0);
        assertThat(q2.completionDays()).isEqualTo(1.5);
    }

    @Test
    void crossCycleTrends_reconciliationsNeverDriveNetMissingBelowZero() {
        Audit a = closedAudit("odd cycle", "2026-03-01T00:00:00Z", "2026-03-01T06:00:00Z"); // 0.2 days
        stubCounts(a, 50, 2, 5); // more reconciliations than missing (data anomaly) must clamp at 0
        when(auditService.list(AuditStatus.CLOSED)).thenReturn(List.of(a));

        AuditAnalyticsService.AuditCycleTrend cycle = service.crossCycleTrends().get(0);

        assertThat(cycle.netMissingCount()).isZero();
        assertThat(cycle.netMissingRatePct()).isZero();
    }

    @Test
    void crossCycleTrends_zeroExpectedYieldsZeroRateNotDivideByZero() {
        Audit a = closedAudit("empty scope", "2026-04-01T00:00:00Z", "2026-04-01T01:00:00Z");
        stubCounts(a, 0, 0, 0);
        when(auditService.list(AuditStatus.CLOSED)).thenReturn(List.of(a));

        AuditAnalyticsService.AuditCycleTrend cycle = service.crossCycleTrends().get(0);

        assertThat(cycle.missingRatePct()).isZero();
        assertThat(cycle.netMissingRatePct()).isZero();
    }

    @Test
    void crossCycleTrends_noClosedAuditsIsEmpty() {
        when(auditService.list(AuditStatus.CLOSED)).thenReturn(List.of());
        assertThat(service.crossCycleTrends()).isEmpty();
    }
}
