package com.iams.analytics.application;

import com.iams.analytics.domain.FeedbackCategory;
import com.iams.analytics.domain.FeedbackItem;
import com.iams.analytics.domain.FeedbackItemRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-ANL-04: stores the feedback item and raises the event that routes it to
 * the configured recipient. Open to every authenticated user - the story's
 * persona is an Employee/Volunteer, the least-privileged role in the system.
 */
@Service
public class FeedbackService {

    static final int MAX_MESSAGE_LENGTH = 2000;

    private final FeedbackItemRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public FeedbackService(FeedbackItemRepository repository, CurrentUserProvider currentUserProvider,
                           ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FeedbackItem submit(FeedbackCategory category, String message, String pagePath) {
        if (category == null) {
            throw ValidationFailedException.singleField("category", "This field is required");
        }
        // Blank text collapses to null: category alone is accepted per the AC.
        String normalizedMessage = message == null || message.isBlank() ? null : message.trim();
        if (normalizedMessage != null && normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            throw ValidationFailedException.singleField("message",
                    "Must be at most " + MAX_MESSAGE_LENGTH + " characters");
        }

        CurrentUser submitter = currentUserProvider.current();
        FeedbackItem item = new FeedbackItem();
        item.setCategory(category);
        item.setMessage(normalizedMessage);
        item.setPagePath(pagePath == null || pagePath.isBlank() ? null : pagePath.trim());
        item.setSubmittedBy(submitter.id());
        item = repository.save(item);

        // Synchronous, like every notification-producing business event: the routed
        // notification rows commit or roll back atomically with the feedback item.
        eventPublisher.publishEvent(new FeedbackSubmittedEvent(item.getId(), category.name(), normalizedMessage,
                submitter.id(), submitter.username(), item.getPagePath()));
        return item;
    }
}
