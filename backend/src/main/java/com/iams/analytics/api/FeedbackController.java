package com.iams.analytics.api;

import com.iams.analytics.application.FeedbackService;
import com.iams.analytics.application.TrackUsage;
import com.iams.analytics.domain.FeedbackCategory;
import com.iams.analytics.domain.FeedbackItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-ANL-04. No @PreAuthorize: any authenticated user may submit feedback -
 * the story's persona is Employee/Volunteer, and there is nothing here to
 * protect (a user can only create their own feedback, never read anyone's).
 * <p>
 * US-ANL-02 note: this is the analytics module's ONLY inbound API, and it
 * accepts user-authored feedback exclusively. There is deliberately no
 * endpoint through which a client can submit usage/telemetry events - usage
 * capture is server-side only (UsageTrackingInterceptor), which is what
 * makes "no client-side event-submission API" structurally true rather than
 * a convention.
 */
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @TrackUsage(module = "feedback", action = "submit")
    public ResponseEntity<FeedbackResponse> submit(@Valid @RequestBody FeedbackRequest request) {
        FeedbackItem item = feedbackService.submit(request.category(), request.message(), request.pagePath());
        return ResponseEntity.status(201).body(FeedbackResponse.from(item));
    }

    public record FeedbackRequest(@NotNull FeedbackCategory category,
                                  @Size(max = 2000) String message,
                                  @Size(max = 255) String pagePath) {
    }

    /** The 201 body IS the receipt confirmation the AC asks for. */
    public record FeedbackResponse(UUID id, FeedbackCategory category, String message, String pagePath,
                                   Instant createdAt) {
        static FeedbackResponse from(FeedbackItem item) {
            return new FeedbackResponse(item.getId(), item.getCategory(), item.getMessage(), item.getPagePath(),
                    item.getCreatedAt());
        }
    }
}
