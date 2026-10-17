package com.fixflow.controller;

import com.fixflow.dto.request.AnalyzeLogRequest;
import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.dto.response.PreviewResponse;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.parser.ParsedLog;
import com.fixflow.service.AnalysisService;
import com.fixflow.service.KnowledgeBaseService;
import com.fixflow.service.LogInputService;
import com.fixflow.service.LogInputService.PreparedUpload;
import com.fixflow.service.LogParserService;
import com.fixflow.service.RuleBasedDiagnosisService;
import com.fixflow.service.SecretRedactionService;
import com.fixflow.service.SecretRedactionService.RedactionResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/diagnoses")
public class DiagnosisController {

    private static final String WARNING =
            "Automatic secret detection is heuristic and not perfect. Review the sanitized text before analysis.";

    private final LogInputService inputService;
    private final SecretRedactionService redactionService;
    private final LogParserService parserService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RuleBasedDiagnosisService ruleBasedDiagnosisService;
    private final AnalysisService analysisService;

    public DiagnosisController(LogInputService inputService,
                               SecretRedactionService redactionService,
                               LogParserService parserService,
                               KnowledgeBaseService knowledgeBaseService,
                               RuleBasedDiagnosisService ruleBasedDiagnosisService,
                               AnalysisService analysisService) {
        this.inputService = inputService;
        this.redactionService = redactionService;
        this.parserService = parserService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.ruleBasedDiagnosisService = ruleBasedDiagnosisService;
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<DiagnosisResponse> analyze(@Valid @RequestBody AnalyzeLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analysisService.analyzePasted(request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiagnosisResponse> analyzeUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(analysisService.analyzeUpload(file));
    }

    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody AnalyzeLogRequest request) {
        String normalized = inputService.preparePastedContent(request.content());
        return buildPreview(normalized, null);
    }

    @PostMapping(value = "/preview/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreviewResponse previewUpload(@RequestParam("file") MultipartFile file) {
        PreparedUpload upload = inputService.prepareUpload(file);
        return buildPreview(upload.content(), upload.fileName());
    }

    /** Deterministic analysis without any AI call. Nothing is saved. */
    @PostMapping("/rule-based-preview")
    public DiagnosisResponse ruleBasedPreview(@Valid @RequestBody AnalyzeLogRequest request) {
        String normalized = inputService.preparePastedContent(request.content());
        RedactionResult redacted = redactionService.redact(normalized);
        ParsedLog parsed = parserService.parse(redacted.sanitizedText());
        List<KnowledgeArticle> articles = knowledgeBaseService.findRelevant(parsed);
        return ruleBasedDiagnosisService.diagnose(parsed, articles);
    }

    private PreviewResponse buildPreview(String normalizedContent, String fileName) {
        RedactionResult redacted = redactionService.redact(normalizedContent);
        ParsedLog parsed = parserService.parse(redacted.sanitizedText());
        return new PreviewResponse(
                redacted.sanitizedText(),
                fileName,
                redacted.totalCount(),
                redacted.countsByType(),
                WARNING,
                parsed);
    }
}