package com.fixflow.service;

import com.fixflow.ai.AiDiagnosisPayload;
import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.dto.response.DiagnosisResponse.KnowledgeReference;
import com.fixflow.dto.response.DiagnosisResponse.ProbableCause;
import com.fixflow.dto.response.DiagnosisResponse.RecommendedStep;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.Level;
import com.fixflow.entity.Risk;
import com.fixflow.entity.Severity;
import com.fixflow.parser.ParsedLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Checks the model output against the schema and against the evidence. Returns problems as readable
 * messages so they can be sent back in a repair prompt.
 */
@Service
public class DiagnosisValidator {

    public record Outcome(DiagnosisResponse response, List<String> errors) {
        public boolean valid() {
            return response != null && errors.isEmpty();
        }
    }

    private static final int MAX_CAUSES = 6;
    private static final int MAX_STEPS = 10;
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_.$]{6,}");
    private static final Pattern EXECUTION_CLAIM = Pattern.compile(
            "(?i)\\b(?:i|we|fixflow)\\s+(?:have\\s+|has\\s+|already\\s+)?"
                    + "(?:fixed|executed|ran|applied|changed|deleted|restarted|updated|modified)\\b");
    private static final String NOTE =
            "AI-generated suggestion. FixFlow checked its format and references, but the content is not verified. "
                    + "Review every step before applying it.";
    private static final String DEFAULT_FIX =
            "No specific fix can be proposed until more information is provided.";

    public Outcome validate(AiDiagnosisPayload payload, ParsedLog parsed, String sanitizedText,
                            Map<String, String> allowedArticles) {
        if (payload == null) {
            return new Outcome(null, List.of("The response was empty."));
        }
        List<String> errors = new ArrayList<>();

        String summary = requiredText(payload.summary(), "summary", 1000, errors);
        ErrorCategory category = parseEnum(payload.category(), ErrorCategory.class, "category", errors);
        Severity severity = parseEnum(payload.severity(), Severity.class, "severity", errors);
        Level confidence = parseEnum(payload.confidenceLevel(), Level.class, "confidenceLevel", errors);
        String confidenceExplanation = requiredText(payload.confidenceExplanation(), "confidenceExplanation", 1000, errors);
        boolean needsMore = Boolean.TRUE.equals(payload.needsMoreInformation());
        String groundingText = (sanitizedText + " " + String.join(" ", allowedArticles.values())).toLowerCase();

        List<ProbableCause> causes = validateCauses(payload.probableCauses(), needsMore, groundingText, errors);
        List<RecommendedStep> steps = validateSteps(payload.recommendedSteps(), needsMore, errors);
        String suggestedFix = validateFix(payload.suggestedFix(), needsMore, errors);

        List<String> verification = textList(payload.verificationSteps(), "verificationSteps", 8, 400, errors);
        if (!needsMore && verification.isEmpty()) {
            errors.add("verificationSteps must list at least one step when needsMoreInformation is false");
        }
        List<String> limitations = textList(payload.limitations(), "limitations", 6, 400, errors);
        if (limitations.isEmpty()) {
            errors.add("limitations must list at least one limitation");
        }
        List<String> followUps = textList(payload.followUpQuestions(), "followUpQuestions", 5, 300, errors);
        if (needsMore && followUps.isEmpty()) {
            errors.add("followUpQuestions must contain a question when needsMoreInformation is true");
        }
        List<KnowledgeReference> references = validateReferences(payload.knowledgeReferences(), allowedArticles, errors);

        if (confidence == Level.HIGH && needsMore) {
            errors.add("confidenceLevel HIGH is not allowed when needsMoreInformation is true");
        }
        if (confidence == Level.HIGH && !parsed.hasEvidence()) {
            errors.add("confidenceLevel HIGH is not allowed when the log contains no recognizable error evidence");
        }
        checkNoExecutionClaims(errors, summary, suggestedFix, steps, verification);

        if (!errors.isEmpty()) {
            return new Outcome(null, List.copyOf(errors));
        }
        return new Outcome(new DiagnosisResponse(null, summary, category, severity, confidence,
                confidenceExplanation, causes, steps, suggestedFix, verification, references, limitations,
                needsMore, followUps, AnalysisMode.AI, NOTE), List.of());
    }

    private List<ProbableCause> validateCauses(List<AiDiagnosisPayload.Cause> items, boolean needsMore,
                                               String groundingText, List<String> errors) {
        List<AiDiagnosisPayload.Cause> causes = items == null ? List.of() : items;
        if (causes.size() > MAX_CAUSES) {
            errors.add("probableCauses must contain at most " + MAX_CAUSES + " items");
        }
        if (!needsMore && causes.isEmpty()) {
            errors.add("probableCauses must contain at least one cause when needsMoreInformation is false");
        }
        List<ProbableCause> result = new ArrayList<>();
        for (int i = 0; i < Math.min(causes.size(), MAX_CAUSES); i++) {
            AiDiagnosisPayload.Cause item = causes.get(i);
            String prefix = "probableCauses[" + i + "]";
            if (item == null) {
                errors.add(prefix + " is empty");
                continue;
            }
            String cause = requiredText(item.cause(), prefix + ".cause", 500, errors);
            Level likelihood = parseEnum(item.likelihood(), Level.class, prefix + ".likelihood", errors);
            List<String> evidence = textList(item.evidence(), prefix + ".evidence", 6, 500, errors);
            if (!needsMore && evidence.isEmpty()) {
                errors.add(prefix + ".evidence must contain at least one item");
            }
            if (!evidence.isEmpty() && evidence.stream().noneMatch(text -> isGrounded(text, groundingText))) {
                errors.add(prefix + ".evidence does not match anything in the log; quote real details from the log");
            }
            result.add(new ProbableCause(cause, evidence, likelihood));
        }
        return result;
    }

    private List<RecommendedStep> validateSteps(List<AiDiagnosisPayload.Step> items, boolean needsMore,
                                                List<String> errors) {
        List<AiDiagnosisPayload.Step> steps = items == null ? List.of() : items.stream().filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(step -> step.order() == null ? Integer.MAX_VALUE : step.order()))
                .toList();
        if (steps.size() > MAX_STEPS) {
            errors.add("recommendedSteps must contain at most " + MAX_STEPS + " items");
        }
        if (!needsMore && steps.isEmpty()) {
            errors.add("recommendedSteps must contain at least one step when needsMoreInformation is false");
        }
        List<RecommendedStep> result = new ArrayList<>();
        for (int i = 0; i < Math.min(steps.size(), MAX_STEPS); i++) {
            AiDiagnosisPayload.Step step = steps.get(i);
            String prefix = "recommendedSteps[" + i + "]";
            String action = requiredText(step.action(), prefix + ".action", 500, errors);
            String reason = requiredText(step.reason(), prefix + ".reason", 500, errors);
            Risk risk = parseEnum(step.risk(), Risk.class, prefix + ".risk", errors);
            result.add(new RecommendedStep(i + 1, action, reason, risk));
        }
        return result;
    }

    private String validateFix(String fix, boolean needsMore, List<String> errors) {
        if (needsMore && (fix == null || fix.isBlank())) {
            return DEFAULT_FIX;
        }
        return requiredText(fix, "suggestedFix", 2000, errors);
    }

    private List<KnowledgeReference> validateReferences(List<AiDiagnosisPayload.Reference> items,
                                                        Map<String, String> allowedArticles, List<String> errors) {
        if (items == null) {
            return List.of();
        }
        Map<String, KnowledgeReference> unique = new LinkedHashMap<>();
        for (AiDiagnosisPayload.Reference item : items) {
            if (item == null || item.articleId() == null || item.articleId().isBlank()) {
                continue;
            }
            String id = item.articleId().strip();
            String title = allowedArticles.get(id);
            if (title == null) {
                errors.add("knowledgeReferences contains unknown articleId '" + shorten(id)
                        + "'; use only guide IDs that were provided");
                continue;
            }
            unique.putIfAbsent(id, new KnowledgeReference(id, title));
        }
        return List.copyOf(unique.values());
    }

    private void checkNoExecutionClaims(List<String> errors, String summary, String fix,
                                        List<RecommendedStep> steps, List<String> verification) {
        StringBuilder all = new StringBuilder(summary).append(' ').append(fix);
        steps.forEach(step -> all.append(' ').append(step.action()).append(' ').append(step.reason()));
        verification.forEach(text -> all.append(' ').append(text));
        if (EXECUTION_CLAIM.matcher(all).find()) {
            errors.add("The response must not claim that anything was fixed, run, or changed; only propose steps for the user to take");
        }
    }

    private boolean isGrounded(String evidence, String groundingText) {
        Matcher matcher = WORD.matcher(evidence);
        while (matcher.find()) {
            if (groundingText.contains(matcher.group().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type, String field, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(field + " is required");
            return null;
        }
        try {
            return Enum.valueOf(type, value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add(field + " must be one of " + Arrays.toString(type.getEnumConstants()));
            return null;
        }
    }

    private String requiredText(String value, String field, int maxLength, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(field + " is required");
            return "";
        }
        if (value.length() > maxLength) {
            errors.add(field + " is too long (maximum " + maxLength + " characters)");
        }
        return value.strip();
    }

    private List<String> textList(List<String> values, String field, int maxItems, int maxLength, List<String> errors) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > maxItems) {
            errors.add(field + " must contain at most " + maxItems + " items");
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (value.length() > maxLength) {
                errors.add(field + " contains an item longer than " + maxLength + " characters");
                continue;
            }
            result.add(value.strip());
        }
        return result;
    }

    private String shorten(String value) {
        return value.length() <= 60 ? value : value.substring(0, 60);
    }
}