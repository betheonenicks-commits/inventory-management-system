package com.iams.analytics.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.iams.analytics.application.TrackUsage;
import com.iams.analytics.application.UsageRecorder;
import com.iams.common.security.CurrentUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
class UsageTrackingInterceptorTest {

    @Mock private UsageRecorder recorder;

    private UsageTrackingInterceptor interceptor;
    private CurrentUser user;

    // Stand-in controller: the interceptor only cares about the annotation.
    static class DummyController {
        @TrackUsage(module = "assets", action = "list-register")
        public void tracked() {
        }

        public void untracked() {
        }
    }

    @BeforeEach
    void setUp() {
        interceptor = new UsageTrackingInterceptor(recorder);
        user = new CurrentUser(UUID.randomUUID(), "im", Set.of("INVENTORY_MANAGER"), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new DummyController(), DummyController.class.getMethod(methodName));
    }

    @Test
    void successfulAnnotatedRequest_isRecorded() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(new MockHttpServletRequest(), response, handler("tracked"), null);

        verify(recorder).record(user, "assets", "list-register");
    }

    @Test
    void failedRequest_isNotRecorded() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        interceptor.afterCompletion(new MockHttpServletRequest(), response, handler("tracked"), null);

        verify(recorder, never()).record(any(), anyString(), anyString());
    }

    @Test
    void handlerException_isNotRecorded() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(new MockHttpServletRequest(), response, handler("tracked"),
                new RuntimeException("boom"));

        verify(recorder, never()).record(any(), anyString(), anyString());
    }

    @Test
    void unannotatedHandler_isNotRecorded() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(new MockHttpServletRequest(), response, handler("untracked"), null);

        verify(recorder, never()).record(any(), anyString(), anyString());
    }

    @Test
    void unauthenticatedCompletion_isNotRecordedAndDoesNotFail() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        assertThatCode(() -> interceptor.afterCompletion(new MockHttpServletRequest(), response,
                handler("tracked"), null)).doesNotThrowAnyException();
        verify(recorder, never()).record(any(), anyString(), anyString());
    }

    @Test
    void recorderFailure_neverPropagatesToTheRequestThread() throws Exception {
        doThrow(new RuntimeException("unexpected")).when(recorder).record(any(), anyString(), anyString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        assertThatCode(() -> interceptor.afterCompletion(new MockHttpServletRequest(), response,
                handler("tracked"), null)).doesNotThrowAnyException();
    }
}
