package com.fininsight.config;

/**
 * Central registry of cache names used across the application.
 */
public final class CacheNames {

    private CacheNames() {
        // Prevent instantiation
    }

    public static final String DASHBOARD = "dashboard";
    public static final String ANALYTICS_SUMMARY = "analytics:summary";
    public static final String ANALYTICS_CATEGORY = "analytics:category";
    public static final String ANALYTICS_MONTHLY = "analytics:monthly";
    public static final String ANALYTICS_BUDGET_OVERVIEW = "analytics:budget-overview";
    public static final String ANALYTICS_TOP_CATEGORIES = "analytics:top-categories";
    public static final String CATEGORIES = "categories";
    public static final String AI_INSIGHTS = "ai:insights";
}
