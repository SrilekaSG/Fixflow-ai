package com.fixflow.service;

import com.fixflow.ai.AiDiagnosisPayload;
import com.fixflow.ai.AiPromptBuilder;
import com.fixflow.ai.AiProvider;
import com.fixflow.ai.AiResponseParser;
import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.DiagnosisStatus;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.InvalidAiResponseException;
import com.fixflow.parser.ParsedLog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs the fixed workflow: search guides, try the AI (if configured), validate, retry once, and otherwise
 * fall back to the rule-based diagnosis. It only ever receives sanitized text.
 */
@Service
public class AgentOrchestrator {

    public record Result(DiagnosisResponse response, AnalysisMode mode, DiagnosisStatus status,
                         String providerName, String modelName, List<KnowledgeArticle> matchedArticles) {
    }

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final RuleBasedDiagnosisService ruleBasedDiagnosisService;
    private final AiProvider aiProvider;
    private final AiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final DiagnosisValidator validator;

    public AgentOrchestrator(KnowledgeBaseService knowledgeBaseService,
                             RuleBasedDiagnosisService ruleBasedDiagnosisService,
                             AiProvider aiProvider,
                             AiPromptBuilder promptBuilder,
                             AiResponseParser responseParser,
                             DiagnosisValidator validator) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.ruleBasedDiagnosisService = ruleBasedDiagnosisService;
        this.aiProvider = aiProvider;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.validator = validator;
    }

    public Result run(String sanitizedText, ParsedLog parsed) {
        List<KnowledgeArticle> articles = knowledgeBaseService.findRelevant(parsed);

        if (!aiProvider.isAvailable()) {
            return ruleBased(parsed, articles, DiagnosisStatus.COMPLETED,
                    "AI analysis is not configured (AI_PROVIDER is disabled), so this rule-based result was used.");
        }

        try {
            DiagnosisResponse response = generateWithAi(sanitizedText, parsed, articles);
            return new Result(response, AnalysisMode.AI, DiagnosisStatus.COMPLETED,
                    aiProvider.providerName(), aiProvider.modelName(), articles);
        } catch (AiProviderException e) {
            log.warn("AI provider call failed: {}", e.getReason());
            return ruleBased(parsed, articles, DiagnosisStatus.PARTIAL,
                    "AI analysis was unavailable: " + e.getMessage() + " Showing the rule-based result instead.");
        } catch (InvalidAiResponseException e) {
            log.warn("AI response stayed invalid after one repair attempt");
            return ruleBased(parsed, articles, DiagnosisStatus.PARTIAL,
                    "The AI returned an invalid response even after one repair attempt. "
                            + "Showing the rule-based result instead.");
        }
    }

    private DiagnosisResponse generateWithAi(String sanitizedText, ParsedLog parsed, List<KnowledgeArticle> articles) {
        Map<String, String> allowedArticles = new LinkedHashMap<>();
        articles.forEach(article -> allowedArticles.putIfAbsent(article.getId(), article.getTitle()));

        String systemPrompt = promptBuilder.systemPrompt();
        String userPrompt = promptBuilder.userPrompt(parsed, sanitizedText, articles);

        DiagnosisValidator.Outcome first = validateRaw(aiProvider.generate(systemPrompt, userPrompt),
                parsed, sanitizedText, allowedArticles);
        if (first.valid()) {
            return first.response();
        }

        log.info("AI response failed validation ({} problem(s)); retrying once with a repair prompt",
                first.errors().size());
        String repairPrompt = promptBuilder.repairPrompt(userPrompt, first.errors());
        DiagnosisValidator.Outcome second = validateRaw(aiProvider.generate(systemPrompt, repairPrompt),
                parsed, sanitizedText, allowedArticles);
        if (second.valid()) {
            return second.response();
        }
        throw new InvalidAiResponseException("The AI response was still invalid after one repair attempt.");
    }

    private DiagnosisValidator.Outcome validateRaw(String raw, ParsedLog parsed, String sanitizedText,
                                                   Map<String, String> allowedArticles) {
        try {
            AiDiagnosisPayload payload = responseParser.parse(raw);
            return validator.validate(payload, parsed, sanitizedText, allowedArticles);
        } catch (InvalidAiResponseException e) {
            return new DiagnosisValidator.Outcome(null, List.of(e.getMessage()));
        }
    }

    private Result ruleBased(ParsedLog parsed, List<KnowledgeArticle> articles, DiagnosisStatus status, String prefix) {
        DiagnosisResponse base = ruleBasedDiagnosisService.diagnose(parsed, articles);
        DiagnosisResponse labelled = base.withAnalysisNote(prefix + " " + base.analysisNote());
        return new Result(labelled, AnalysisMode.RULE_BASED, status, null, null, articles);
    }
}