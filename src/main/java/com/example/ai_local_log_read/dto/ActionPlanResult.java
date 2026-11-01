package com.example.ai_local_log_read.dto;

import java.util.List;

public record ActionPlanResult(
        List<String> recommendedActions,  // Yapılması gereken adımlar
        String priority,                  // Önem Derecesi (IMMEDIATE, SCHEDULED, MONITOR)
        boolean requiresHumanReview       // İnsan onayı gerekir mi
) {}
