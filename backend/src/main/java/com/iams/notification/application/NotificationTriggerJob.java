package com.iams.notification.application;

import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditAssignmentRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.dashboard.application.DashboardQueries;
import com.iams.inventory.application.InventoryStockService;
import com.iams.lifecycle.domain.AssetTransferRequest;
import com.iams.lifecycle.domain.AssetTransferRequestRepository;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationTriggerLog;
import com.iams.notification.domain.NotificationTriggerLogRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-NTF-06: the standard trigger catalog, evaluated on a schedule. Every
 * rule follows one shape: query the owning module's existing repositories,
 * claim (eventType, entityId, thresholdKey) in the trigger ledger, and
 * dispatch only if the claim won - the DB unique constraint makes
 * "exactly once per threshold" true even across overlapping sweeps.
 * Recipients are resolved at fire time (US-NTF-07), never stored.
 */
@Component
public class NotificationTriggerJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationTriggerJob.class);

    private final AuditRepository auditRepository;
    private final AuditAssignmentRepository auditAssignmentRepository;
    private final AssetTransferRequestRepository transferRepository;
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final DashboardQueries dashboardQueries;
    private final InventoryStockService stockService;
    private final TriggerClaimService claimService;
    private final NotificationDispatchService dispatchService;
    private final RecipientResolverService recipientResolver;
    private final NotificationProperties properties;

    public NotificationTriggerJob(AuditRepository auditRepository,
                                  AuditAssignmentRepository auditAssignmentRepository,
                                  AssetTransferRequestRepository transferRepository,
                                  MaintenanceScheduleRepository maintenanceScheduleRepository,
                                  DashboardQueries dashboardQueries, InventoryStockService stockService,
                                  TriggerClaimService claimService,
                                  NotificationDispatchService dispatchService,
                                  RecipientResolverService recipientResolver, NotificationProperties properties) {
        this.auditRepository = auditRepository;
        this.auditAssignmentRepository = auditAssignmentRepository;
        this.transferRepository = transferRepository;
        this.maintenanceScheduleRepository = maintenanceScheduleRepository;
        this.dashboardQueries = dashboardQueries;
        this.stockService = stockService;
        this.claimService = claimService;
        this.dispatchService = dispatchService;
        this.recipientResolver = recipientResolver;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${iams.notifications.trigger-sweep-ms:3600000}",
            initialDelayString = "${iams.notifications.trigger-initial-delay-ms:60000}")
    public void sweep() {
        int fired = sweepNow();
        if (fired > 0) {
            log.info("Notification trigger sweep fired {} notification group(s)", fired);
        }
    }

    /**
     * Deliberately NOT @Transactional as a whole: each claim runs REQUIRES_NEW
     * through TriggerClaimService's proxy and each dispatch owns its own
     * transaction, so one lost race or one bad rule can never poison the rest
     * of the sweep.
     */
    public int sweepNow() {
        LocalDate today = LocalDate.now();
        int fired = 0;
        fired += upcomingAudits(today);
        fired += overdueAudits(today);
        fired += expiries(today);
        fired += maintenanceDue(today);
        fired += lowStock();
        fired += pendingApprovals();
        return fired;
    }

    private int upcomingAudits(LocalDate today) {
        int fired = 0;
        for (int days : new int[] {properties.getUpcomingAuditDaysFirst(), properties.getUpcomingAuditDaysSecond()}) {
            LocalDate target = today.plusDays(days);
            for (Audit audit : auditRepository.findScheduledBetween(target, target)) {
                if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
                    continue;
                }
                if (claim(NotificationEventType.UPCOMING_AUDIT, audit.getId(), days + "d")) {
                    Map<String, String> vars = Map.of("auditName", audit.getName(),
                            "when", days == 1 ? "tomorrow" : "in " + days + " days",
                            "scheduledDate", String.valueOf(audit.getScheduledDate()));
                    dispatchService.dispatch(NotificationEventType.UPCOMING_AUDIT, auditRecipients(audit), vars,
                            "/audits/" + audit.getId());
                    fired++;
                }
            }
        }
        return fired;
    }

    private int overdueAudits(LocalDate today) {
        int fired = 0;
        for (Audit audit : auditRepository.findScheduledBetween(today.minusYears(5), today.minusDays(1))) {
            if (audit.getStatus() == AuditStatus.CLOSED || audit.getScheduledDate() == null) {
                continue;
            }
            long daysOverdue = ChronoUnit.DAYS.between(audit.getScheduledDate(), today);
            // Repeats every N days until closed: one threshold key per repeat window.
            long window = daysOverdue / properties.getOverdueRepeatDays();
            if (claim(NotificationEventType.OVERDUE_AUDIT, audit.getId(), "overdue#" + window)) {
                Map<String, String> vars = Map.of("auditName", audit.getName(),
                        "scheduledDate", String.valueOf(audit.getScheduledDate()),
                        "daysOverdue", String.valueOf(daysOverdue));
                dispatchService.dispatch(NotificationEventType.OVERDUE_AUDIT, auditRecipients(audit), vars,
                        "/audits/" + audit.getId());
                fired++;
            }
        }
        return fired;
    }

    private int expiries(LocalDate today) {
        int fired = 0;
        LocalDate horizon = today.plusDays(properties.getExpiryLookaheadDays());
        List<DashboardQueries.ExpiringEntry> entries = new java.util.ArrayList<>();
        Map<UUID, String> kinds = new HashMap<>();
        for (DashboardQueries.ExpiringEntry e : dashboardQueries.warrantyExpirations(today, horizon, null)) {
            entries.add(e);
            kinds.put(e.assetId(), "Warranty");
        }
        for (DashboardQueries.ExpiringEntry e : dashboardQueries.insuranceExpirations(today, horizon, null)) {
            entries.add(e);
            kinds.putIfAbsent(e.assetId(), "Insurance");
        }
        for (DashboardQueries.ExpiringEntry entry : entries) {
            String kind = kinds.getOrDefault(entry.assetId(), "Expiry");
            if (claim(NotificationEventType.EXPIRY, entry.assetId(), kind + "@" + entry.dueDate())) {
                Map<String, String> vars = Map.of("kind", kind, "assetName", entry.assetName(),
                        "dueDate", String.valueOf(entry.dueDate()),
                        "detail", entry.detail() == null ? "" : entry.detail());
                dispatchService.dispatch(NotificationEventType.EXPIRY,
                        recipientResolver.roleHoldersCoveringScope("INVENTORY_MANAGER", null), vars,
                        "/assets/" + entry.assetId());
                fired++;
            }
        }
        return fired;
    }

    private int maintenanceDue(LocalDate today) {
        int fired = 0;
        for (MaintenanceSchedule schedule : maintenanceScheduleRepository.findDueOnOrBefore(today.plusDays(7))) {
            if (claim(NotificationEventType.MAINTENANCE_DUE, schedule.getId(), "due@" + schedule.getNextDueDate())) {
                Map<String, String> vars = Map.of("assetName", schedule.getAsset().getName(),
                        "dueDate", String.valueOf(schedule.getNextDueDate()),
                        "detail", schedule.getDescription() == null ? "" : schedule.getDescription());
                dispatchService.dispatch(NotificationEventType.MAINTENANCE_DUE,
                        recipientResolver.roleHoldersCoveringScope("INVENTORY_MANAGER", null), vars,
                        "/assets/" + schedule.getAsset().getId());
                fired++;
            }
        }
        return fired;
    }

    private int lowStock() {
        int fired = 0;
        LocalDate today = LocalDate.now();
        for (InventoryStockService.LowStockItem item : stockService.lowStockItems()) {
            // Once per item per day while it stays below - re-fires after it recovers and drops again on a later day.
            if (claim(NotificationEventType.LOW_STOCK, item.item().getId(), "below@" + today)) {
                Map<String, String> vars = Map.of("itemName", item.item().getName(),
                        "quantity", String.valueOf(item.totalQuantity()),
                        "unit", item.item().getUnitOfMeasure() == null ? "" : item.item().getUnitOfMeasure().name().toLowerCase(),
                        "reorderLevel", String.valueOf(item.item().getReorderLevel()));
                dispatchService.dispatch(NotificationEventType.LOW_STOCK,
                        recipientResolver.roleHoldersCoveringScope("INVENTORY_MANAGER", null), vars, "/inventory");
                fired++;
            }
        }
        return fired;
    }

    private int pendingApprovals() {
        int fired = 0;
        for (AssetTransferRequest transfer : transferRepository
                .findByStatusWithAssociationsOrderByRequestedAtDesc(LifecycleRequestStatus.PENDING)) {
            if (claim(NotificationEventType.PENDING_APPROVAL, transfer.getId(), "pending")) {
                UUID approver = transfer.getEffectiveApproverId() != null ? transfer.getEffectiveApproverId()
                        : transfer.getNominalApproverId();
                Map<String, String> vars = Map.of("entityType", "Asset transfer",
                        "summary", "A transfer of " + transfer.getAsset().getName(),
                        "since", String.valueOf(transfer.getRequestedAt()));
                dispatchService.dispatch(NotificationEventType.PENDING_APPROVAL,
                        Set.of(recipientResolver.effective(approver)), vars,
                        "/assets/" + transfer.getAsset().getId());
                fired++;
            }
        }
        for (Audit audit : auditRepository
                .findByStatusWithAssociationsOrderByCreatedAtDesc(AuditStatus.PENDING_APPROVAL)) {
            if (claim(NotificationEventType.PENDING_APPROVAL, audit.getId(), "pending")) {
                UUID approver = audit.getEffectiveApproverId() != null ? audit.getEffectiveApproverId()
                        : audit.getNominalApproverId();
                Map<String, String> vars = Map.of("entityType", "Audit",
                        "summary", "Audit \"" + audit.getName() + "\"",
                        "since", String.valueOf(audit.getSubmittedAt()));
                dispatchService.dispatch(NotificationEventType.PENDING_APPROVAL,
                        Set.of(recipientResolver.effective(approver)), vars, "/audits/" + audit.getId());
                fired++;
            }
        }
        return fired;
    }

    private Set<UUID> auditRecipients(Audit audit) {
        Set<UUID> recipients = auditAssignmentRepository.findByAuditIdOrderByCreatedAtAsc(audit.getId()).stream()
                .filter(AuditAssignment::isActive)
                .map(AuditAssignment::getAuditorUserId)
                .collect(Collectors.toSet());
        if (recipients.isEmpty() && audit.getCreatedBy() != null) {
            recipients = Set.of(audit.getCreatedBy());
        }
        return recipientResolver.resolveMany(List.copyOf(recipients)).stream().collect(Collectors.toSet());
    }

    private boolean claim(NotificationEventType eventType, UUID entityId, String thresholdKey) {
        return claimService.claim(eventType, entityId, thresholdKey);
    }
}
