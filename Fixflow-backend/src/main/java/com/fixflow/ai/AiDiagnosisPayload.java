package com.fixflow.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** The raw shape the model is asked to return. Everything is a plain string so validation can explain problems. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiDiagnosisPayload(
        String summary,
        String category,
        String severity,
        String confidenceLevel,
        String confidenceExplanation,
        List<Cause> probableCauses,
        List<Step> recommendedSteps,
        String suggestedFix,
        List<String> verificationSteps,
        List<Reference> knowledgeReferences,
        List<String> limitations,
        Boolean needsMoreInformation,
        List<String> followUpQuestions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cause(String cause, List<String> evidence, String likelihood) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(Integer order, String action, String reason, String risk) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reference(String articleId, String title) {
    }
}