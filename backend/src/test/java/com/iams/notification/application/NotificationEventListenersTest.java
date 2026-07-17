package com.iams.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.analytics.application.FeedbackSubmittedEvent;
import com.iams.notification.domain.NotificationEventType;
import com.iams.usr.domain.AppUserRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** US-ANL-04's routing leg; the transfer/assignment listeners are covered by their producing services' tests. */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenersTest {

    @Mock private NotificationDispatchService dispatchService;
    @Mock private RecipientResolverService recipientResolver;
    @Mock private AppUserRepository userRepository;

    private NotificationProperties properties;
    private NotificationEventListeners listeners;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        listeners = new NotificationEventListeners(dispatchService, recipientResolver, userRepository, properties);
    }

    private FeedbackSubmittedEvent event(String message, String pagePath) {
        return new FeedbackSubmittedEvent(UUID.randomUUID(), "FRICTION", message, UUID.randomUUID(), "emp", pagePath);
    }

    @Test
    void feedback_routesToConfiguredRoleHoldersWithContentInTheBodyVariables() {
        Set<UUID> admins = Set.of(UUID.randomUUID());
        when(recipientResolver.roleHoldersCoveringScope("ADMIN", null)).thenReturn(admins);

        listeners.onFeedbackSubmitted(event("the table is slow", "/assets"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(dispatchService).dispatch(eq(NotificationEventType.FEEDBACK_RECEIVED), eq(admins), vars.capture(),
                isNull());
        assertThat(vars.getValue())
                .containsEntry("category", "FRICTION")
                .containsEntry("submitter", "emp")
                .containsEntry("message", "the table is slow")
                .containsEntry("pageContext", " (from /assets)");
    }

    @Test
    void feedback_fallsBackToAdminsWhenTheConfiguredRoleHasNoHolders() {
        properties.setFeedbackRecipientRole("HELPDESK");
        Set<UUID> admins = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(recipientResolver.roleHoldersCoveringScope("HELPDESK", null)).thenReturn(Set.of());
        when(recipientResolver.admins()).thenReturn(admins);

        listeners.onFeedbackSubmitted(event("text", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(dispatchService).dispatch(eq(NotificationEventType.FEEDBACK_RECEIVED), eq(admins), vars.capture(),
                isNull());
    }

    @Test
    void feedback_categoryOnlySubmission_rendersAnExplicitNoTextMarker() {
        when(recipientResolver.roleHoldersCoveringScope("ADMIN", null)).thenReturn(Set.of(UUID.randomUUID()));

        listeners.onFeedbackSubmitted(event(null, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> vars = ArgumentCaptor.forClass(Map.class);
        verify(dispatchService).dispatch(eq(NotificationEventType.FEEDBACK_RECEIVED), org.mockito.ArgumentMatchers.any(),
                vars.capture(), isNull());
        assertThat(vars.getValue())
                .containsEntry("message", "(no text - category only)")
                .containsEntry("pageContext", "");
    }
}
