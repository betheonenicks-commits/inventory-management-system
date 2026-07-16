package com.iams.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationTemplate;
import com.iams.notification.domain.NotificationTemplateRepository;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock private NotificationTemplateRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;

    private NotificationTemplateService service;

    @BeforeEach
    void setUp() {
        service = new NotificationTemplateService(repository, currentUserProvider);
        lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "admin", Set.of("SUPER_ADMIN"), Set.of("*")));
    }

    private NotificationTemplate template(int version, String subject, String body) {
        NotificationTemplate t = new NotificationTemplate();
        t.setEventType(NotificationEventType.LOW_STOCK);
        t.setChannel(NotificationChannel.EMAIL);
        t.setVersion(version);
        t.setSubject(subject);
        t.setBody(body);
        return t;
    }

    @Test
    void render_substitutesVariables() {
        when(repository.findFirstByEventTypeAndChannelOrderByVersionDesc(NotificationEventType.LOW_STOCK,
                NotificationChannel.EMAIL))
                .thenReturn(Optional.of(template(1, "Low stock: {{itemName}}", "{{itemName}} at {{quantity}}")));

        NotificationTemplateService.Rendered rendered = service.render(NotificationEventType.LOW_STOCK,
                NotificationChannel.EMAIL, Map.of("itemName", "Gloves", "quantity", "3"));

        assertThat(rendered.subject()).isEqualTo("Low stock: Gloves");
        assertThat(rendered.body()).isEqualTo("Gloves at 3");
    }

    @Test
    void render_failsSafeOnMissingVariable() {
        when(repository.findFirstByEventTypeAndChannelOrderByVersionDesc(any(), any()))
                .thenReturn(Optional.of(template(1, "Low stock: {{itemName}}", "at {{quantity}}")));

        NotificationTemplateService.Rendered rendered =
                service.render(NotificationEventType.LOW_STOCK, NotificationChannel.EMAIL, Map.of());

        // The AC: a clear placeholder, never a broken message or an exception.
        assertThat(rendered.subject()).isEqualTo("Low stock: [missing: itemName]");
        assertThat(rendered.body()).isEqualTo("at [missing: quantity]");
    }

    @Test
    void saveNewVersion_incrementsFromLatestAndNeverEdits() {
        when(repository.findFirstByEventTypeAndChannelOrderByVersionDesc(NotificationEventType.LOW_STOCK,
                NotificationChannel.EMAIL)).thenReturn(Optional.of(template(3, "s", "b")));
        when(repository.save(any(NotificationTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationTemplate saved = service.saveNewVersion(NotificationEventType.LOW_STOCK,
                NotificationChannel.EMAIL, "New subject", "New body");

        assertThat(saved.getVersion()).isEqualTo(4);
        assertThat(saved.getSubject()).isEqualTo("New subject");
    }

    @Test
    void saveNewVersion_rejectsBlankSubject() {
        assertThatThrownBy(() -> service.saveNewVersion(NotificationEventType.LOW_STOCK, NotificationChannel.EMAIL,
                "  ", "body")).isInstanceOf(ValidationFailedException.class);
    }
}
