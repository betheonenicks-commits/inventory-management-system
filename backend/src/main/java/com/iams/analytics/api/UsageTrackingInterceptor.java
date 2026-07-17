package com.iams.analytics.api;

import com.iams.analytics.application.TrackUsage;
import com.iams.analytics.application.UsageRecorder;
import com.iams.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * US-ANL-01's capture point. afterCompletion runs after the response is
 * committed, so the business action has already completed normally before
 * capture is even attempted - the ordering, not just the try/catch, is what
 * makes "analytics never blocks a user-facing operation" true. Only
 * successful (2xx) completions of @TrackUsage-annotated handlers by an
 * authenticated CurrentUser are recorded.
 */
@Component
public class UsageTrackingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UsageTrackingInterceptor.class);

    private final UsageRecorder recorder;

    public UsageTrackingInterceptor(UsageRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        try {
            if (ex != null || response.getStatus() < 200 || response.getStatus() >= 300
                    || !(handler instanceof HandlerMethod handlerMethod)) {
                return;
            }
            TrackUsage annotation = handlerMethod.getMethodAnnotation(TrackUsage.class);
            if (annotation == null) {
                return;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
                recorder.record(user, annotation.module(), annotation.action());
            }
        } catch (Exception e) {
            // Belt to UsageRecorder's braces - nothing in this path may ever surface.
            log.warn("Usage tracking interceptor failed: {}", e.getMessage());
        }
    }
}
