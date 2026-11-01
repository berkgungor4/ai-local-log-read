package com.example.ai_local_log_read.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResult {
    List<String> summaryLines;
    int totalTokens;
    long executionTimeMs;
    private Map<String, Integer> maskedItemsCount;
}
