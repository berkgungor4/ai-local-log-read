package com.example.ai_local_log_read.dto;

public record PipelineAnalysisResult(
        ClassificationResult classification,
        RootCauseResult rootCause,
        ActionPlanResult actionPlan,
        long classificationTimeMs,
        long rootCauseTimeMs,
        long actionPlanTimeMs,
        long totalTimeMs
) {}