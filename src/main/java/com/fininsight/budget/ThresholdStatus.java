package com.fininsight.budget;

import java.math.BigDecimal;

/**
 * Budget spending threshold status.
 * - NORMAL: spent < 80% of budget
 * - WARNING: 80% <= spent < 100% of budget
 * - EXCEEDED: spent >= 100% of budget
 */
public enum ThresholdStatus {
    NORMAL,
    WARNING,
    EXCEEDED;

    private static final BigDecimal WARNING_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal EXCEEDED_THRESHOLD = BigDecimal.valueOf(100);

    public static ThresholdStatus fromPercentage(BigDecimal usagePercentage) {
        if (usagePercentage == null || usagePercentage.compareTo(WARNING_THRESHOLD) < 0) {
            return NORMAL;
        }
        if (usagePercentage.compareTo(EXCEEDED_THRESHOLD) < 0) {
            return WARNING;
        }
        return EXCEEDED;
    }
}
