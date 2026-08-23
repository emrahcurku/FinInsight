package com.fininsight.ai.dto;

import java.util.Collections;
import java.util.List;

/**
 * Raw generation result returned by AI provider clients before response assembly.
 */
public record AiGenerationResult(
        String summary,
        List<AiInsightItem> insights,
        List<String> recommendations,
        String model
) {
    public AiGenerationResult {
        summary = summary != null ? summary : "";
        insights = insights != null ? Collections.unmodifiableList(insights) : Collections.emptyList();
        recommendations = recommendations != null ? Collections.unmodifiableList(recommendations) : Collections.emptyList();
        model = model != null ? model : "unknown";
    }
}
