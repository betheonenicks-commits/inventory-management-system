package com.iams.notification.application;

import com.iams.analytics.application.FeedbackSubmittedEvent;
import com.iams.asset.application.AssetAssignmentChangedEvent;
import com.iams.lifecycle.application.TransferDecidedEvent;
import com.iams.notification.domain.NotificationEventType;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * US-NTF-04: turns business events into notifications. Synchronous listeners
 * on purpose - dispatch only writes rows, so the notification commits or
 * rolls back atomically with the business change that raised it ("when the
 * assignment commits, then ... notified").
 */
@Component
public class NotificationEventListeners {

    private final NotificationDispatchService dispatchService;
    private final RecipientResolverService recipientResolver;
    private final AppUserRepository userRepository;
    private final NotificationProperties properties;

    public NotificationEventListeners(NotificationDispatchService dispatchService,
                                      RecipientResolverService recipientResolver, AppUserRepository userRepository,
                                      NotificationProperties properties) {
        this.dispatchService = dispatchService;
        this.recipientResolver = recipientResolver;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @EventListener
    public void onTransferDecided(TransferDecidedEvent event) {
        Set<UUID> recipients = new LinkedHashSet<>();
        recipients.add(recipientResolver.effective(event.requesterUserId()));
        userForPerson(event.fromPersonId()).ifPresent(recipients::add);
        userForPerson(event.toPersonId()).ifPresent(recipients::add);

        Map<String, String> vars = new HashMap<>();
        vars.put("assetName", event.assetName());
        vars.put("decision", event.decision());
        vars.put("actor", event.actorUsername());
        vars.put("reason", event.reason() == null || event.reason().isBlank() ? "" : "Reason: " + event.reason());
        // The transfer UI lives on the asset's detail page - there is no standalone
        // /transfers route (found by the browser test's click-through, not assumed).
        dispatchService.dispatch(NotificationEventType.TRANSFER_DECISION, recipients, vars,
                "/assets/" + event.assetId());
    }

    @EventListener
    public void onAssignmentChanged(AssetAssignmentChangedEvent event) {
        Set<UUID> recipients = new LinkedHashSet<>();
        userForPerson(event.personId()).ifPresent(recipients::add);
        if (recipients.isEmpty()) {
            // The person has no login account - nobody to notify, not an error.
            return;
        }
        Map<String, String> vars = Map.of("assetNumber", event.assetNumber(), "assetName", event.assetName(),
                "action", event.action(), "personName",
                "assigned to".equals(event.action()) ? event.personName() : "from " + event.personName(),
                "actor", event.actorUsername());
        dispatchService.dispatch(NotificationEventType.ASSIGNMENT, recipients, vars, "/assets/" + event.assetId());
    }

    /**
     * US-ANL-04: feedback routes to whoever holds the configured recipient
     * role right now (resolved at send time like every other recipient set);
     * if nobody holds it - a misconfigured custom role, say - it falls back
     * to admins rather than vanishing.
     */
    @EventListener
    public void onFeedbackSubmitted(FeedbackSubmittedEvent event) {
        Set<UUID> recipients = recipientResolver.roleHoldersCoveringScope(properties.getFeedbackRecipientRole(), null);
        if (recipients.isEmpty()) {
            recipients = recipientResolver.admins();
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("category", event.category());
        vars.put("submitter", event.submitterUsername());
        vars.put("message", event.message() == null ? "(no text - category only)" : event.message());
        vars.put("pageContext", event.pagePath() == null ? "" : " (from " + event.pagePath() + ")");
        dispatchService.dispatch(NotificationEventType.FEEDBACK_RECEIVED, recipients, vars, null);
    }

    private java.util.Optional<UUID> userForPerson(UUID personId) {
        if (personId == null) {
            return java.util.Optional.empty();
        }
        return userRepository.findByPersonId(personId).map(AppUser::getId);
    }
}
