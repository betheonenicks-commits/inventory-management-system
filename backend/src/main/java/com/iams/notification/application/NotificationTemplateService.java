package com.iams.notification.application;

import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.notification.domain.NotificationChannel;
import com.iams.notification.domain.NotificationEventType;
import com.iams.notification.domain.NotificationTemplate;
import com.iams.notification.domain.NotificationTemplateRepository;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-NTF-09: versioned templates with {{variable}} substitution. Rendering
 * fails SAFE per the AC - a variable not supplied at send time renders as an
 * explicit "[missing: name]" marker, never a broken half-message and never an
 * exception that would strand the notification.
 */
@Service
public class NotificationTemplateService {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    private final NotificationTemplateRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public NotificationTemplateService(NotificationTemplateRepository repository,
                                       CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Rendered render(NotificationEventType eventType, NotificationChannel channel, Map<String, String> vars) {
        NotificationTemplate template = repository
                .findFirstByEventTypeAndChannelOrderByVersionDesc(eventType, channel)
                .orElseThrow(() -> new IllegalStateException(
                        "No template seeded for " + eventType + "/" + channel + " - V43 seeds every catalog pair"));
        return new Rendered(substitute(template.getSubject(), vars), substitute(template.getBody(), vars),
                template.getVersion());
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> list() {
        return repository.findAllByOrderByEventTypeAscChannelAscVersionDesc();
    }

    /** Saving an edit inserts version N+1 - templates are immutable rows, never updated (US-NTF-09). */
    @Transactional
    public NotificationTemplate saveNewVersion(NotificationEventType eventType, NotificationChannel channel,
                                               String subject, String body) {
        if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
            throw ValidationFailedException.singleField(subject == null || subject.isBlank() ? "subject" : "body",
                    "must not be blank");
        }
        int latest = repository.findFirstByEventTypeAndChannelOrderByVersionDesc(eventType, channel)
                .map(NotificationTemplate::getVersion).orElse(0);
        NotificationTemplate template = new NotificationTemplate();
        template.setEventType(eventType);
        template.setChannel(channel);
        template.setVersion(latest + 1);
        template.setSubject(subject.trim());
        template.setBody(body.trim());
        template.setCreatedBy(currentUserProvider.current().id());
        return repository.save(template);
    }

    private static String substitute(String text, Map<String, String> vars) {
        Matcher matcher = VARIABLE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = vars.get(name);
            matcher.appendReplacement(sb,
                    Matcher.quoteReplacement(value != null ? value : "[missing: " + name + "]"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public record Rendered(String subject, String body, int templateVersion) {
    }
}
