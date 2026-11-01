package com.example.ai_local_log_read.dto;

public record RootCauseResult(
        String rootCause,              // Tespit edilen kök neden
        String affectedComponent,      // Etkilenen servis
        boolean historicalMatchFound,  // Geçmişte benzer vaka bulundu mu
        String historicalContext       // Bulunduysa geçmiş kayıt özeti
) {}
