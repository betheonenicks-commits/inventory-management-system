package com.iams.analytics.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * US-ANL-01: marks a controller method as a tracked feature-usage action.
 * Capture happens server-side in UsageTrackingInterceptor after a successful
 * (2xx) completion - no client-side tracking script exists anywhere (the
 * point of FR-ANL-01), and a capture failure can never fail the request.
 * <p>
 * The tracked catalog is deliberately curated: one annotation per
 * user-meaningful action (run a report, register an asset, scan), not every
 * endpoint - extending it is adding one annotation, never new pipeline code.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackUsage {

    /** Module bucket the adoption report (US-ANL-03) groups by, e.g. "assets", "reports". */
    String module();

    /** The action within the module, e.g. "register", "run-asset-register". */
    String action();
}
