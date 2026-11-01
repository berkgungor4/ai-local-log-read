package com.example.ai_local_log_read.security;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LogSanitizer {
    // IPv4 adresleri
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");

    // E-posta adresleri
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}");

    // JWT/Bearer token benzeri uzun alfanumerik string'ler
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("(?i)(bearer\\s+)?[A-Za-z0-9-_]{20,}\\.[A-Za-z0-9-_]{10,}\\.[A-Za-z0-9-_]{10,}");

    // Kredi kartı numarası formatı
    private static final Pattern CREDIT_CARD_PATTERN =
            Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");

    // Türkiye TC Kimlik No formatı
    private static final Pattern TC_KIMLIK_PATTERN =
            Pattern.compile("\\b[1-9][0-9]{10}\\b");

    // Telefon numarası
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b(\\+90|0)?\\s?5\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}\\b");

    /**
     * Verilen log içeriğini maskeleyerek döner.
     */
    public String sanitize(String rawLog) {
        String result = rawLog;

        result = TOKEN_PATTERN.matcher(result).replaceAll("[MASKED_TOKEN]");
        result = EMAIL_PATTERN.matcher(result).replaceAll("[MASKED_EMAIL]");
        result = IP_PATTERN.matcher(result).replaceAll("[MASKED_IP]");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[MASKED_CARD]");
        result = TC_KIMLIK_PATTERN.matcher(result).replaceAll("[MASKED_TC_ID]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[MASKED_PHONE]");

        return result;
    }

    /**
     * "Kaç adet hassas veri tespit edip maskeledim" bilgisini raporlama.
     */
    public Map<String, Integer> countMaskedItems(String rawLog) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("IP", countMatches(IP_PATTERN, rawLog));
        counts.put("EMAIL", countMatches(EMAIL_PATTERN, rawLog));
        counts.put("TOKEN", countMatches(TOKEN_PATTERN, rawLog));
        counts.put("CREDIT_CARD", countMatches(CREDIT_CARD_PATTERN, rawLog));
        counts.put("TC_KIMLIK", countMatches(TC_KIMLIK_PATTERN, rawLog));
        counts.put("PHONE", countMatches(PHONE_PATTERN, rawLog));
        return counts;
    }

    private int countMatches(Pattern pattern, String text) {
        var matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
