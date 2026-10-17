package com.fixflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.dto.request.AnalyzeLogRequest;
import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.Diagnosis;
import com.fixflow.parser.ParsedLog;
import com.fixflow.repository.DiagnosisRepository;
import com.fixflow.service.LogInputService.PreparedUpload;
import com.fixflow.service.SecretRedactionService.RedactionResult;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * End-to-end workflow. Input is always redacted before it is parsed, sent to an AI provider, or saved.
 */
@Service
public class AnalysisService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_EXCEPTION_CLASS_LENGTH = 300;

    private final LogInputService inputService;
    private final SecretRedactionService redactionService;
    private final LogParserService parserService;
    private final AgentOrchestrator orchestrator;
    private final DiagnosisRepository diagnosisRepository;
    private final ObjectMapper objectMapper;

    public AnalysisService(LogInputService inputService,
                           SecretRedactionService redactionService,
                           LogParserService parserService,
                           AgentOrchestrator orchestrator,
                           DiagnosisRepository diagnosisRepository,
                           ObjectMapper objectMapper) {
        this.inputService = inputService;
        this.redactionService = redactionService;
        this.parserService = parserService;
        this.orchestrator = orchestrator;
        this.diagnosisRepository = diagnosisRepository;
        this.objectMapper = objectMapper;
    }

    public DiagnosisResponse analyzePasted(AnalyzeLogRequest request) {
        String normalized = inputService.preparePastedContent(request.content());
        String title = firstNonBlank(request.title(), request.sourceLabel());
        return analyze(normalized, null, title);
    }

    public DiagnosisResponse analyzeUpload(MultipartFile file) {
        PreparedUpload upload = inputService.prepareUpload(file);
        return analyze(upload.content(), upload.fileName(), upload.fileName());
    }

    private DiagnosisResponse analyze(String normalizedContent, String fileName, String requestedTitle) {
        RedactionResult redacted = redactionService.redact(normalizedContent);
        String sanitized = redacted.sanitizedText();
        ParsedLog parsed = parserService.parse(sanitized);

        AgentOrchestrator.Result result = orchestrator.run(sanitized, parsed);

        Diagnosis saved = diagnosisRepository.save(new Diagnosis(
                truncate(requestedTitle != null ? requestedTitle : defaultTitle(parsed), MAX_TITLE_LENGTH),
                sanitized,
                fileName,
                truncate(parsed.exceptionClass(), MAX_EXCEPTION_CLASS_LENGTH),
                parsed.errorMessage(),
                result.response().category(),
                result.response().severity(),
                toJson(result.response()),
                result.mode(),
                result.mode() == AnalysisMode.AI ? result.providerName() : null,
                result.mode() == AnalysisMode.AI ? result.modelName() : null,
                result.status(),
                redacted.totalCount(),
                new HashSet<>(result.matchedArticles())));

        return result.response().withDiagnosisId(saved.getId());
    }

    private String toJson(DiagnosisResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the diagnosis result", e);
        }
    }

    private String defaultTitle(ParsedLog parsed) {
        if (parsed.exceptionClass() == null) {
            return "Log analysis";
        }
        String className = parsed.exceptionClass();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.strip();
        }
        if (second != null && !second.isBlank()) {
            return second.strip();
        }
        return null;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
