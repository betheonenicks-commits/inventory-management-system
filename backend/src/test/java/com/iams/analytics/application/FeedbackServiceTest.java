package com.iams.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.iams.analytics.domain.FeedbackCategory;
import com.iams.analytics.domain.FeedbackItem;
import com.iams.analytics.domain.FeedbackItemRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackItemRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FeedbackService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(repository, currentUserProvider, eventPublisher);
        userId = UUID.randomUUID();
        lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(userId, "emp", Set.of("EMPLOYEE_VOLUNTEER"), Set.of()));
        lenient().when(repository.save(any(FeedbackItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submit_savesTrimmedItemAndPublishesRoutingEvent() {
        service.submit(FeedbackCategory.FRICTION, "  the asset table is slow  ", "/assets");

        ArgumentCaptor<FeedbackItem> item = ArgumentCaptor.forClass(FeedbackItem.class);
        verify(repository).save(item.capture());
        assertThat(item.getValue().getMessage()).isEqualTo("the asset table is slow");
        assertThat(item.getValue().getSubmittedBy()).isEqualTo(userId);
        assertThat(item.getValue().getPagePath()).isEqualTo("/assets");

        ArgumentCaptor<FeedbackSubmittedEvent> event = ArgumentCaptor.forClass(FeedbackSubmittedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().category()).isEqualTo("FRICTION");
        assertThat(event.getValue().message()).isEqualTo("the asset table is slow");
        assertThat(event.getValue().submitterUsername()).isEqualTo("emp");
    }

    @Test
    void submit_categoryAloneIsAccepted_blankMessageCollapsesToNull() {
        service.submit(FeedbackCategory.PRAISE, "   ", null);

        ArgumentCaptor<FeedbackItem> item = ArgumentCaptor.forClass(FeedbackItem.class);
        verify(repository).save(item.capture());
        assertThat(item.getValue().getMessage()).isNull();
        assertThat(item.getValue().getPagePath()).isNull();

        ArgumentCaptor<FeedbackSubmittedEvent> event = ArgumentCaptor.forClass(FeedbackSubmittedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message()).isNull();
    }

    @Test
    void submit_missingCategoryIsRejected() {
        assertThatThrownBy(() -> service.submit(null, "text", null))
                .isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void submit_oversizedMessageIsRejected() {
        assertThatThrownBy(() -> service.submit(FeedbackCategory.BUG, "x".repeat(2001), null))
                .isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(repository, eventPublisher);
    }
}
