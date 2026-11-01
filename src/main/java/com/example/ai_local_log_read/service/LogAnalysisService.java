package com.example.ai_local_log_read.service;

import com.example.ai_local_log_read.dto.*;
import com.example.ai_local_log_read.security.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogAnalysisService {

    private final LogInvestigationTools logInvestigationTools;
    private final ChatClient chatClient;
    private final LogSanitizer logSanitizer;
    private final VectorStore vectorStore;

    public AnalysisResult analyzeLogFile(MultipartFile file) {
        try {
            //Dosyayı String'e çevir
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            //AI'ya göndermeden maskeleme
            String sanitizedContent = logSanitizer.sanitize(content);

            //AI'ya gönder
            long startTime = System.currentTimeMillis();

            ChatResponse response = chatClient.prompt()
                    .user(u -> u.text("""
                Analyze the log below. You MUST provide your answer in the following format; do not add anything else.:

                SUMMARY: (single sentence)
                POSSIBLE CAUSE: (single sentence)
                RECOMMENDED ACTION: (single sentence)

                Log:
                {logContent}
                """)
                            .param("logContent", sanitizedContent))
                    .call()
                    .chatResponse();

            long endTime = System.currentTimeMillis();

            //Metrikleri al
            String output = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata().getUsage();

            List<String> summaryLines = Arrays.stream(output.split("\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();

            AnalysisResult analysisResult = AnalysisResult.builder()
                    .summaryLines(summaryLines)
                    .totalTokens(usage.getTotalTokens())
                    .executionTimeMs(endTime - startTime)
                    .maskedItemsCount(logSanitizer.countMaskedItems(content))
                    .build();

            return analysisResult;

        } catch (IOException e) {
            throw new RuntimeException("File read error", e);
        }
    }

    public AnalysisResult analyzeWithAutonomousAgent(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            String sanitizedContent = logSanitizer.sanitize(content);

            long startTime = System.currentTimeMillis();

            // Ajanı çalıştırıyoruz ve Spring AI üzerinden aracı (tool) bağlıyoruz
            ChatResponse response = chatClient.prompt()
                    .system("""
        You are an expert DevOps Autonomous Agent. When analyzing logs, query past solutions using the
        'searchHistoricalErrors' tool if necessary.

        Your answer MUST be in this format:
        SUMMARY: (single sentence)
        POSSIBLE CAUSE: (single sentence)
        PAST RECORDS: (briefly if found, "none" if not found)
        RECOMMENDED ACTION: (single sentence)
        """)
                    .user("Analyze these logs, find the root cause, and use historical database data if necessary: " + sanitizedContent)
                    .tools(logInvestigationTools) // AI'ın kullanabileceği Java yeteneği
                    .call()
                    .chatResponse();

            long endTime = System.currentTimeMillis();
            String output = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata().getUsage();

            List<String> summaryLines = Arrays.stream(output.split("\\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();

            AnalysisResult analysisResult = AnalysisResult.builder()
                    .summaryLines(summaryLines)
                    .totalTokens(usage.getTotalTokens())
                    .executionTimeMs(endTime - startTime)
                    .maskedItemsCount(logSanitizer.countMaskedItems(content))
                    .build();

            return analysisResult;

        } catch (Exception e) {
            throw new RuntimeException("The autonomous agent could not be started", e);
        }
    }

    public PipelineAnalysisResult analyzeWithMultiAgentPipeline(MultipartFile file) {
        try {
            String rawContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            String sanitizedContent = logSanitizer.sanitize(rawContent);

            long pipelineStart = System.currentTimeMillis();

            // 1.Aşama: Classifier Agent
            long stage1Start = System.currentTimeMillis();
            ClassificationResult classification = runClassifierAgent(sanitizedContent);
            long classificationTime = System.currentTimeMillis() - stage1Start;

            log.info("[PIPELINE] Classifier completed -> severity: {}, category: {}",
                    classification.severity(), classification.category());

            // 2.Aşama:: RCA Agent (tool use aktif)
            long stage2Start = System.currentTimeMillis();
            RootCauseResult rootCause = runRcaAgent(sanitizedContent, classification);
            long rootCauseTime = System.currentTimeMillis() - stage2Start;

            log.info("[PIPELINE] RCA completed -> rootCause: {}", rootCause.rootCause());

            // 3.Aşama:: Action Agent
            long stage3Start = System.currentTimeMillis();
            ActionPlanResult actionPlan = runActionAgent(classification, rootCause);
            long actionPlanTime = System.currentTimeMillis() - stage3Start;

            log.info("[PIPELINE] Action Plan completed -> priority: {}", actionPlan.priority());

            persistAnalysisToKnowledgeBase(classification, rootCause);

            long totalTime = System.currentTimeMillis() - pipelineStart;

            return new PipelineAnalysisResult(
                    classification, rootCause, actionPlan,
                    classificationTime, rootCauseTime, actionPlanTime, totalTime
            );

        } catch (IOException e) {
            throw new RuntimeException("File read error", e);
        }
    }

    /**
     * Logu okuyup türünü ve önem derecesini belirleme.
     * Tool kullanmaz — sadece sınıflandırma yapar, hızlı olmalı.
     */
    private ClassificationResult runClassifierAgent(String sanitizedContent) {
        return chatClient.prompt()
                .system("""
                        You are a log classification expert. Your task is ONLY
                        to determine the severity level and category of the log.
                        DO NOT perform a root cause analysis; that is another agent's job.
                        Provide the answer according to the requested diagram.
                        """)
                .user(u -> u.text("Classify this log: {logContent}")
                        .param("logContent", sanitizedContent))
                .call()
                .entity(ClassificationResult.class);
    }

    /**
     * Kök nedeni bulur. Gerekirse geçmiş arıza veritabanını (tool) sorgular.
     * Bir önceki ajanın sınıflandırmasını context olarak alır.
     */
    private RootCauseResult runRcaAgent(String sanitizedContent, ClassificationResult classification) {
        return chatClient.prompt()
                .system("""
                        You are an expert DevOps root cause analysis (RCA) agent.
                        The previous agent classified this log as follows:
                        Severity: %s, Category: %s

                        Use this context to identify the root cause.
                        If necessary, use the 'searchHistoricalErrors' tool to
                        check if there has been a similar case in the past.
                        """.formatted(classification.severity(), classification.category()))
                .user(u -> u.text("Log content: {logContent}")
                        .param("logContent", sanitizedContent))
                .tools(logInvestigationTools) // Tool use burada aktif
                .call()
                .entity(RootCauseResult.class);
    }

    /**
     * Önceki iki ajanın çıktısına bakarak uygulanabilir aksiyonlar üretir.
     * Ham logu tekrar görmez sadece özetlenmiş bilgiyle çalışıyor.
     * Bu hem token tasarrufu sağlar hem de "her ajan sadece ihtiyacı olan bilgiyi görür" prensibini gösterir.
     */
    private ActionPlanResult runActionAgent(ClassificationResult classification, RootCauseResult rootCause) {
        return chatClient.prompt()
                .system("""
                        You are an action planning agent. Your task is to
                        generate actionable, clear steps based on the classification
                        and root cause information provided to you. Suggest steps
                        tailored to this specific situation, rather than general advice.
                        """)
                .user(u -> u.text("""
                        Classification:
                        - Severity: {severity}
                        - Category: {category}

                        Root Cause Analysis:
                        - Root Cause: {rootCause}
                        - Affected Component: {affectedComponent}
                        - Were Any Past Records Found: {historicalMatch}
                        - Past Context: {historicalContext}

                        Create an action plan based on this information.
                        """)
                        .param("severity", classification.severity())
                        .param("category", classification.category())
                        .param("rootCause", rootCause.rootCause())
                        .param("affectedComponent", rootCause.affectedComponent())
                        .param("historicalMatch", rootCause.historicalMatchFound())
                        .param("historicalContext", rootCause.historicalContext()))
                .call()
                .entity(ActionPlanResult.class);
    }

    /**
     * Yeni yapılan analizi vector store'a kaydeder.
     * Sadece isAnomaly=true olan (yani gerçek bir sorun tespit edilen) analizler kaydedilir.
     */
    private void persistAnalysisToKnowledgeBase(ClassificationResult classification,
                                                RootCauseResult rootCause) {
        if (!classification.isAnomaly()) {
            return;
        }

        String content = String.format(
                "%s error (%s category): %s. Affected component: %s.",
                classification.category(),
                classification.severity(),
                rootCause.rootCause(),
                rootCause.affectedComponent()
        );

        Document newRecord = new Document(content, Map.of(
                "errorType", classification.category(),
                "component", rootCause.affectedComponent(),
                "source", "AUTO_GENERATED", // seed data'dan ayırt etmek için
                "severity", classification.severity()
        ));

        vectorStore.add(List.of(newRecord));
        log.info("[RAG] The new analysis result has been added to the knowledge base: {}", classification.category());
    }
}