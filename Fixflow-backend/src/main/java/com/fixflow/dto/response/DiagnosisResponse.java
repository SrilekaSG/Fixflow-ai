package com.fixflow.dto.response;

import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.Level;
import com.fixflow.entity.Risk;
import com.fixflow.entity.Severity;
import java.util.List;
import java.util.UUID;

public record DiagnosisResponse(
        UUID diagnosisId,
        String summary,
        ErrorCategory category,
        Severity severity,
        Level confidenceLevel,
        String confidenceExplanation,
        List<ProbableCause> probableCauses,
        List<RecommendedStep> recommendedSteps,
        String suggestedFix,
        List<String> verificationSteps,
        List<KnowledgeReference> knowledgeReferences,
        List<String> limitations,
        boolean needsMoreInformation,
        List<String> followUpQuestions,
        AnalysisMode analysisMode,
        String analysisNote) {

    public record ProbableCause(String cause, List<String> evidence, Level likelihood) {
    }

    public record RecommendedStep(int order, String action, String reason, Risk risk) {
    }

    public record KnowledgeReference(String articleId, String title) {
    }

    public DiagnosisResponse withDiagnosisId(UUID id) {
        return new DiagnosisResponse(id, summary, category, severity, confidenceLevel, confidenceExplanation,
                probableCauses, recommendedSteps, suggestedFix, verificationSteps, knowledgeReferences,
                limitations, needsMoreInformation, followUpQuestions, analysisMode, analysisNote);
    }

    public DiagnosisResponse withAnalysisNote(String note) {
        return new DiagnosisResponse(diagnosisId, summary, category, severity, confidenceLevel,
                confidenceExplanation, probableCauses, recommendedSteps, suggestedFix, verificationSteps,
                knowledgeReferences, limitations, needsMoreInformation, followUpQuestions, analysisMode, note);
    }
}