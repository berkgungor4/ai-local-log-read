package com.example.ai_local_log_read.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Uygulama açılışında vector store'u hazır arıza kayıtlarıyla doldurur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricalIncidentSeeder {

    private final VectorStore vectorStore;

    @EventListener(ApplicationReadyEvent.class)
    public void seedKnowledgeBase() {
        List<Document> seedDocuments = List.of(
                new Document(
                        "ORA-01013 hatası: Oracle veritabanı bağlantı zaman aşımı. " +
                                "Bu hata connection pool'un yetersiz timeout ayarından kaynaklanır. " +
                                "Çözüm: HikariCP connection-timeout değerini 30 saniyeden 60 saniyeye çıkarın.",
                        Map.of("errorType", "ORA-01013", "component", "DATABASE", "incidentId", "402")
                ),
                new Document(
                        "NullPointerException ödeme servisinde: User nesnesi null geldiğinde " +
                                "PaymentService.processPayment() metodunda çöküyor. " +
                                "Çözüm: User nesnesi için Optional kontrolü eklenmeli, null check yapılmadan " +
                                "alan erişimi yapılmamalı.",
                        Map.of("errorType", "NullPointerException", "component", "PAYMENT", "incidentId", "891")
                ),
                new Document(
                        "ConnectionTimeoutException: Redis cache sunucusuna bağlanılamıyor. " +
                                "Genellikle network partition veya Redis sunucusunun aşırı yüklenmesinden kaynaklanır. " +
                                "Çözüm: Redis health check endpoint'i eklenip circuit breaker ile fallback " +
                                "mekanizması kurulmalı.",
                        Map.of("errorType", "ConnectionTimeoutException", "component", "CACHE", "incidentId", "553")
                ),
                new Document(
                        "OutOfMemoryError: Heap space yetersiz. Büyük log dosyalarının tamamının " +
                                "belleğe yüklenmesi sonucu oluştu. " +
                                "Çözüm: Dosya işleme stream tabanlı hale getirilmeli, büyük dosyalar chunk'lar " +
                                "halinde okunmalı.",
                        Map.of("errorType", "OutOfMemoryError", "component", "APPLICATION", "incidentId", "677")
                ),
                new Document(
                        "401 Unauthorized: JWT token süresi dolmuş kullanıcı isteklerinde reddediliyor. " +
                                "Auth servisindeki token refresh mekanizması çalışmıyor. " +
                                "Çözüm: Refresh token akışı kontrol edilmeli, token expiry süresi gözden geçirilmeli.",
                        Map.of("errorType", "401 Unauthorized", "component", "SECURITY", "incidentId", "218")
                )
        );

        vectorStore.add(seedDocuments);
        log.info("[RAG] Knowledge base started, {} past fault records have been loaded.",
                seedDocuments.size());
    }
}
