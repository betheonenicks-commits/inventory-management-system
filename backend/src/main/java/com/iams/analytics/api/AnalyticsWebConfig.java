package com.iams.analytics.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the US-ANL-01 capture interceptor on the API surface. The codebase's first (and only) WebMvcConfigurer. */
@Configuration
public class AnalyticsWebConfig implements WebMvcConfigurer {

    private final UsageTrackingInterceptor usageTrackingInterceptor;

    public AnalyticsWebConfig(UsageTrackingInterceptor usageTrackingInterceptor) {
        this.usageTrackingInterceptor = usageTrackingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(usageTrackingInterceptor).addPathPatterns("/api/**");
    }
}
