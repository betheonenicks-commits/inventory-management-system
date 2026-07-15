package com.iams.dashboard.domain;

/**
 * The closed set of KPI tiles a user can put on their dashboard (US-DSH-06).
 * Every tile rides on the single dashboards:read permission rather than its
 * source module's own read permission - a deliberate design decision: these
 * are org-scoped *aggregates* (counts, percentages, due-date lists), and the
 * VIEWER role (board member / finance officer) exists precisely to see them
 * without holding assets:read/audits:read/inventory:read detail access.
 * Detail navigation stays gated by the underlying permissions in the UI.
 */
public enum DashboardTile {
    ASSET_SUMMARY,
    AUDIT_COMPLETION,
    EXPIRATIONS,
    LOW_STOCK,
    ACTIVITY_FEED,
    AUDIT_CALENDAR
}
