package com.example.ai_local_log_read.dto;

public record ClassificationResult(
        String severity,     // CRITICAL, HIGH, MEDIUM, LOW
        String category,     // DATABASE, NETWORK, APPLICATION, SECURITY
        boolean isAnomaly,   // Normal bir log mu, gerçek bir sorun mu
        String reasoning     // Modelin kısa gerekçesi (şeffaflık için)
) {}
