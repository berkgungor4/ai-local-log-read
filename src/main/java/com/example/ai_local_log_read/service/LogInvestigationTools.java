package com.example.ai_local_log_read.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogInvestigationTools {

    private final VectorStore vectorStore;

    @Tool(description = "It semantically searches the historical error database for a specific error code or status " +
            "and reports how it was previously resolved.")
    public String searchHistoricalErrors(
            @ToolParam(description = "Error code, exception name, or brief description of the situation to search for " +
                    "(Örn: ORA-01013, NullPointerException, Redis connection error)")
            String errorCode) {

        log.info(">> [RAG TOOL] Semantic search is being performed in the Vector Store. Query: {}", errorCode);

        // similarityThreshold: 0.0-1.0 arası, ne kadar yüksekse o kadar alakasız sonuçları eler. 0.5 dengeli bir başlangıç değeri.
        SearchRequest searchRequest = SearchRequest.builder()
                .query(errorCode)
                .topK(2)   // En alakalı 2 kayıt yeterli, context'i şişirmemek için
                .similarityThreshold(0.5)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        if (results.isEmpty()) {
            log.info(">> [RAG TOOL] No result found, the similarity threshold was not met.");
            return "No sufficiently similar, previously resolved case associated with this error code was found in the historical records.";
        }

        // Bulunan kayıtları modelin okuyabileceği düz metne dönüştür.
        String formattedResults = results.stream()
                .map(doc -> "- " + doc.getText() +
                        " (Incident ID: " + doc.getMetadata().getOrDefault("incidentId", "N/A") + ")")
                .collect(Collectors.joining("\n"));

        log.info(">> [RAG TOOL] {} relevant past records were found.", results.size());

        return "Relevant records were found in the historical fault database:\n" + formattedResults;
    }
}