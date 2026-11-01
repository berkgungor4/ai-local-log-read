package com.example.ai_local_log_read.controller;

import com.example.ai_local_log_read.dto.AnalysisResult;
import com.example.ai_local_log_read.dto.PipelineAnalysisResult;
import com.example.ai_local_log_read.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResult> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(logAnalysisService.analyzeLogFile(file));
    }

    @PostMapping(value = "/analyzeWithAutonomousAgent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisResult> analyzeWithAutonomousAgent(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(logAnalysisService.analyzeWithAutonomousAgent(file));
    }

    @PostMapping(value = "/analyze/pipeline", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PipelineAnalysisResult> analyzeWithPipeline(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(logAnalysisService.analyzeWithMultiAgentPipeline(file));
    }
}
