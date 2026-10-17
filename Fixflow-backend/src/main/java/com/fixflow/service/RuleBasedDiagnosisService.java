package com.fixflow.service;

import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.dto.response.DiagnosisResponse.KnowledgeReference;
import com.fixflow.dto.response.DiagnosisResponse.ProbableCause;
import com.fixflow.dto.response.DiagnosisResponse.RecommendedStep;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.entity.Level;
import com.fixflow.entity.Risk;
import com.fixflow.entity.Severity;
import com.fixflow.parser.ParsedLog;
import com.fixflow.parser.StackFrame;
import com.fixflow.service.ArticleText.StepText;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Deterministic diagnosis built only from parsed evidence and built-in guides. No AI is involved,
 * and the result is always labelled as rule-based.
 */
@Service
public class RuleBasedDiagnosisService {

    private static final int MAX_EVIDENCE_LENGTH = 300;
    private static final int MAX_CAUSES = 4;
    private static final int MAX_LOG_LINES_AS_EVIDENCE = 2;
    private static final String ANALYSIS_NOTE =
            "Rule-based result: no AI model was used. This is a match against built-in troubleshooting guides.";
    private static final String NO_DIRECT_EVIDENCE =
            "No direct evidence in the log; this is a common cause for this error signature.";

    public DiagnosisResponse diagnose(ParsedLog parsed, List<KnowledgeArticle> matchedArticles) {
        List<String> patterns = ErrorSignatures.orderedPatterns(parsed.keyPatterns());
        String primaryPattern = patterns.isEmpty() ? null : patterns.get(0);
        KnowledgeArticle primaryArticle = primaryPattern == null ? null
                : findArticle(matchedArticles, ErrorSignatures.ARTICLE_BY_PATTERN.get(primaryPattern));

        if (primaryArticle == null) {
            return parsed.exceptionClass() == null
                    ? insufficientEvidence()
                    : unknownException(parsed, matchedArticles);
        }
        return knownSignature(parsed, matchedArticles, primaryPattern, primaryArticle);
    }

    private DiagnosisResponse knownSignature(ParsedLog parsed, List<KnowledgeArticle> matchedArticles,
                                             String primaryPattern, KnowledgeArticle article) {
        boolean specific = ErrorSignatures.SPECIFIC.contains(primaryPattern) && parsed.exceptionClass() != null;
        Level confidence = specific ? Level.HIGH : Level.MODERATE;
        String confidenceExplanation = specific
                ? "The log contains a distinctive, well-known error signature (" + article.getTitle()
                + ") and a parsed exception. This is a pattern match against a built-in guide, not a verified root cause."
                : "The log matches a known error signature (" + article.getTitle()
                + "), but the same message can have several causes, so this is a moderate-confidence estimate.";

        List<String> evidence = buildEvidence(parsed);
        evidence.add("Matched built-in guide: " + article.getTitle());

        List<String> causeTexts = ArticleText.lines(article.getCommonCauses());
        List<ProbableCause> causes = new ArrayList<>();
        for (int i = 0; i < Math.min(causeTexts.size(), MAX_CAUSES); i++) {
            if (i == 0) {
                causes.add(new ProbableCause(causeTexts.get(i), List.copyOf(evidence), specific ? Level.HIGH : Level.MODERATE));
            } else {
                causes.add(new ProbableCause(causeTexts.get(i), List.of(NO_DIRECT_EVIDENCE), Level.LOW));
            }
        }
        if (causes.isEmpty()) {
            causes.add(new ProbableCause(article.getTitle(), List.copyOf(evidence), Level.MODERATE));
        }

        List<StepText> guideSteps = ArticleText.steps(article.getDiagnosticSteps());
        List<RecommendedStep> steps = new ArrayList<>();
        StackFrame frame = firstApplicationFrame(parsed);
        if (frame != null && "NULL_POINTER".equals(primaryPattern)) {
            steps.add(new RecommendedStep(1,
                    "Open " + describeFrame(frame) + " in your code and check which value is null on that line.",
                    "This is the first stack frame that belongs to your own code.", Risk.LOW));
        }
        for (StepText step : guideSteps) {
            steps.add(new RecommendedStep(steps.size() + 1, step.action(), step.reason(), step.risk()));
        }

        List<String> limitations = new ArrayList<>();
        limitations.add("Rule-based analysis matches known error signatures. It cannot read your source code, configuration, or environment.");
        limitations.add("The causes listed are common causes from a built-in guide; the log alone cannot confirm which one applies.");
        List<String> otherTitles = matchedArticles.stream()
                .filter(other -> !other.getId().equals(article.getId()))
                .map(KnowledgeArticle::getTitle)
                .toList();
        if (!otherTitles.isEmpty()) {
            limitations.add("Other error signatures were also found (" + String.join(", ", otherTitles)
                    + "). FixFlow treated the most specific one as the primary diagnosis.");
        }

        return new DiagnosisResponse(
                null,
                buildSummary(parsed, article),
                article.getCategory(),
                ErrorSignatures.SEVERITY_BY_PATTERN.getOrDefault(primaryPattern, Severity.UNKNOWN),
                confidence,
                confidenceExplanation,
                List.copyOf(causes),
                List.copyOf(steps),
                article.getSuggestedFix() + " This is a proposal only: FixFlow has not changed or run anything.",
                ArticleText.lines(article.getVerificationSteps()),
                references(matchedArticles),
                List.copyOf(limitations),
                false,
                List.of(),
                AnalysisMode.RULE_BASED,
                ANALYSIS_NOTE);
    }

    private DiagnosisResponse unknownException(ParsedLog parsed, List<KnowledgeArticle> matchedArticles) {
        List<String> evidence = buildEvidence(parsed);
        List<ProbableCause> causes = List.of(new ProbableCause(
                "The exact cause cannot be determined from this log alone.", List.copyOf(evidence), Level.LOW));

        List<RecommendedStep> steps = new ArrayList<>();
        StackFrame frame = firstApplicationFrame(parsed);
        if (frame != null) {
            steps.add(new RecommendedStep(1,
                    "Open " + describeFrame(frame) + " in your code and inspect the code around that line.",
                    "This is the first stack frame that belongs to your own code.", Risk.LOW));
        }
        steps.add(new RecommendedStep(steps.size() + 1,
                "Read the last \"Caused by\" section of the stack trace (if there is one) and focus on that exception.",
                "The underlying cause is usually at the end of the chain.", Risk.LOW));
        steps.add(new RecommendedStep(steps.size() + 1,
                "Work out which input, configuration value, or earlier step leads to this exception.",
                "Knowing when it happens narrows the cause.", Risk.LOW));
        steps.add(new RecommendedStep(steps.size() + 1,
                "Search the exact exception class and message in the official documentation of the library that throws it.",
                "Library documentation usually explains its own errors.", Risk.LOW));

        List<String> limitations = new ArrayList<>();
        limitations.add("This exception is not covered by FixFlow's built-in guides, so no known fix could be matched.");
        if (!matchedArticles.isEmpty()) {
            limitations.add("The listed guides are only possibly related to this error.");
        }

        return new DiagnosisResponse(
                null,
                "Insufficient evidence for a known diagnosis: the log shows "
                        + describe(parsed.exceptionClass(), parsed.errorMessage()) + ", which is not covered by FixFlow's built-in guides.",
                ErrorCategory.UNKNOWN,
                Severity.UNKNOWN,
                Level.LOW,
                "An exception was found, but it does not match any known error signature, so FixFlow cannot name a cause without guessing.",
                causes,
                List.copyOf(steps),
                "No specific fix can be proposed from this log. Use the steps above to narrow down the cause, "
                        + "then share more detail for a better diagnosis.",
                List.of(),
                references(matchedArticles),
                List.copyOf(limitations),
                true,
                List.of("Can you share the code around the failing line or the configuration involved?",
                        "Does the error happen every time, or only for some input or in some environments?"),
                AnalysisMode.RULE_BASED,
                ANALYSIS_NOTE);
    }

    private DiagnosisResponse insufficientEvidence() {
        return new DiagnosisResponse(
                null,
                "Insufficient evidence: no exception, stack trace, or known error pattern could be found in the pasted text.",
                ErrorCategory.UNKNOWN,
                Severity.UNKNOWN,
                Level.LOW,
                "No parsable exception or known signature was found, so FixFlow cannot propose a cause without guessing.",
                List.of(),
                List.of(new RecommendedStep(1,
                        "Re-run the failing action and copy the complete error output, starting from the first ERROR line.",
                        "More complete evidence allows a more reliable diagnosis.", Risk.LOW)),
                "No fix can be suggested yet because the evidence is insufficient.",
                List.of(),
                List.of(),
                List.of("Nothing could be concluded from the available text."),
                true,
                List.of("Can you paste the full error output, including the stack trace and any \"Caused by\" lines?",
                        "What were you doing when the error happened (starting the app, calling an endpoint, building)?",
                        "Which framework and version are you using (for example Spring Boot 3.x)?"),
                AnalysisMode.RULE_BASED,
                ANALYSIS_NOTE);
    }

    private String buildSummary(ParsedLog parsed, KnowledgeArticle article) {
        StringBuilder summary = new StringBuilder("Likely issue: ").append(article.getTitle()).append(".");
        if (parsed.rootCauseClass() != null) {
            summary.append(" Root cause reported: ").append(parsed.rootCauseClass());
            if (parsed.rootCauseMessage() != null) {
                summary.append(" (").append(truncate(parsed.rootCauseMessage(), 150)).append(")");
            }
            summary.append(".");
        }
        return summary.toString();
    }

    private List<String> buildEvidence(ParsedLog parsed) {
        List<String> evidence = new ArrayList<>();
        if (parsed.exceptionClass() != null) {
            evidence.add(truncate("Exception: " + describe(parsed.exceptionClass(), parsed.errorMessage()), MAX_EVIDENCE_LENGTH));
        }
        boolean rootDiffers = parsed.rootCauseClass() != null
                && !(Objects.equals(parsed.rootCauseClass(), parsed.exceptionClass())
                && Objects.equals(parsed.rootCauseMessage(), parsed.errorMessage()));
        if (rootDiffers) {
            evidence.add(truncate("Root cause: " + describe(parsed.rootCauseClass(), parsed.rootCauseMessage()), MAX_EVIDENCE_LENGTH));
        }
        StackFrame frame = firstApplicationFrame(parsed);
        if (frame != null) {
            evidence.add("First application frame: " + describeFrame(frame));
        }
        if (parsed.exceptionClass() == null) {
            parsed.relevantLines().stream()
                    .limit(MAX_LOG_LINES_AS_EVIDENCE)
                    .forEach(line -> evidence.add(truncate("Log line: " + line, MAX_EVIDENCE_LENGTH)));
        }
        if (evidence.isEmpty()) {
            evidence.add("The log text matches a known error pattern.");
        }
        return evidence;
    }

    private List<KnowledgeReference> references(List<KnowledgeArticle> articles) {
        return articles.stream()
                .map(article -> new KnowledgeReference(article.getId(), article.getTitle()))
                .toList();
    }

    private KnowledgeArticle findArticle(List<KnowledgeArticle> articles, String id) {
        if (id == null) {
            return null;
        }
        return articles.stream().filter(article -> id.equals(article.getId())).findFirst().orElse(null);
    }

    private StackFrame firstApplicationFrame(ParsedLog parsed) {
        return parsed.stackFrames().stream().filter(StackFrame::applicationCode).findFirst().orElse(null);
    }

    private String describeFrame(StackFrame frame) {
        String location = frame.fileName() == null ? "unknown source"
                : frame.fileName() + (frame.lineNumber() == null ? "" : ":" + frame.lineNumber());
        return frame.className() + "." + frame.methodName() + " (" + location + ")";
    }

    private String describe(String className, String message) {
        return message == null ? className : className + ": " + message;
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}