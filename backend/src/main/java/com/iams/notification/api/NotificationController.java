package com.iams.notification.api;

import com.iams.notification.application.NotificationDeliveryJob;
import com.iams.notification.application.NotificationQueryService;
import com.iams.notification.application.NotificationTemplateService;
import com.iams.notification.application.NotificationTriggerJob;
import com.iams.notification.domain.Notification;
import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EPIC-NTF. Personal endpoints carry no @PreAuthorize - own-rows-only is
 * enforced in the service (the DashboardController-preferences precedent);
 * template administration and the manual job triggers ride on
 * notifications:manage (V43, admins only).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService queryService;
    private final NotificationTemplateService templateService;
    private final NotificationTriggerJob triggerJob;
    private final NotificationDeliveryJob deliveryJob;

    public NotificationController(NotificationQueryService queryService,
                                  NotificationTemplateService templateService, NotificationTriggerJob triggerJob,
                                  NotificationDeliveryJob deliveryJob) {
        this.queryService = queryService;
        this.templateService = templateService;
        this.triggerJob = triggerJob;
        this.deliveryJob = deliveryJob;
    }

    @GetMapping
    public Page<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return queryService.list(unreadOnly, page, size).map(NotificationResponse::from);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", queryService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return NotificationResponse.from(queryService.markRead(id));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("marked", queryService.markAllRead());
    }

    @GetMapping("/preferences")
    public List<NotificationQueryService.PreferenceView> preferences() {
        return queryService.preferences();
    }

    @PutMapping("/preferences/{eventType}")
    public NotificationQueryService.PreferenceView updatePreference(@PathVariable NotificationEventType eventType,
            @Valid @RequestBody PreferenceUpdateRequest request) {
        return queryService.updatePreference(eventType, request.enabled());
    }

    // --- Template administration (US-NTF-09) ---

    @GetMapping("/templates")
    @PreAuthorize("@perm.has('notifications:manage')")
    public List<TemplateResponse> templates() {
        return templateService.list().stream().map(TemplateResponse::from).toList();
    }

    @PostMapping("/templates")
    @PreAuthorize("@perm.has('notifications:manage')")
    public TemplateResponse saveTemplate(@Valid @RequestBody TemplateSaveRequest request) {
        return TemplateResponse.from(templateService.saveNewVersion(request.eventType(), request.channel(),
                request.subject(), request.body()));
    }

    // --- Manual job runs (US-NTF-06/08): deterministic testing and ops recovery ---

    @PostMapping("/admin/sweep")
    @PreAuthorize("@perm.has('notifications:manage')")
    public Map<String, Integer> runSweep() {
        return Map.of("fired", triggerJob.sweepNow());
    }

    @PostMapping("/admin/process-deliveries")
    @PreAuthorize("@perm.has('notifications:manage')")
    public Map<String, Integer> processDeliveries() {
        return Map.of("processed", deliveryJob.processDue());
    }

    public record NotificationResponse(UUID id, NotificationEventType eventType, String title, String body,
                                        String resourcePath, Instant createdAt, Instant readAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getEventType(), n.getTitle(), n.getBody(),
                    n.getResourcePath(), n.getCreatedAt(), n.getReadAt());
        }
    }

    public record PreferenceUpdateRequest(@NotNull Boolean emailEnabled) {
        boolean enabled() {
            return Boolean.TRUE.equals(emailEnabled);
        }
    }

    public record TemplateSaveRequest(@NotNull NotificationEventType eventType,
                                       @NotNull NotificationChannel channel,
                                       @NotBlank @Size(max = 200) String subject,
                                       @NotBlank @Size(max = 10000) String body) {
    }

    public record TemplateResponse(UUID id, NotificationEventType eventType, NotificationChannel channel, int version,
                                    String subject, String body, Instant createdAt) {
        static TemplateResponse from(NotificationTemplate t) {
            return new TemplateResponse(t.getId(), t.getEventType(), t.getChannel(), t.getVersion(), t.getSubject(),
                    t.getBody(), t.getCreatedAt());
        }
    }
}
