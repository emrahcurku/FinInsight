package com.fininsight.ai.dto;

import com.fininsight.dashboard.dto.InsightSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Individual financial insight or behavioral recommendation item.
 */
@Schema(description = "Structured financial insight item")
public record AiInsightItem(
        @Schema(description = "Insight category identifier", example = "TOP_SPENDING_CATEGORY")
        String type,

        @Schema(description = "Severity level (INFO, WARNING, DANGER)", example = "INFO")
        InsightSeverity severity,

        @Schema(description = "Short actionable title", example = "High Dining & Food Spending")
        String title,

        @Schema(description = "Detailed contextual description", example = "Your dining spending accounted for 42% of total expenses this month.")
        String description,

        @Schema(description = "Specific, non-investment financial recommendation", example = "Consider setting a weekly dining budget limit to balance expenses.")
        String recommendation
) {
    public AiInsightItem {
        type = type != null ? type : "FINANCIAL_INSIGHT";
        severity = severity != null ? severity : InsightSeverity.INFO;
        title = title != null ? title : "";
        description = description != null ? description : "";
        recommendation = recommendation != null ? recommendation : "";
    }
}
