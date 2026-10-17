package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.ai.AiResponseParser;
import com.fixflow.ai.AiTestData;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.parser.ParsedLog;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosisValidatorTest {

    private static final Map<String, String> ALLOWED =
            Map.of("pg-auth-failed", "PostgreSQL password authentication failed");

    private final AiResponseParser responseParser = new AiResponseParser(new ObjectMapper());
    private final LogParserService logParser = new LogParserService();
    private final DiagnosisValidator validator = new DiagnosisValidator();

    private DiagnosisValidator.Outcome validate(String json) {
        return validate(json, AiTestData.PG_AUTH_LOG);
    }

    private DiagnosisValidator.Outcome validate(String json, String log) {
        ParsedLog parsed = logParser.parse(log);
        return validator.validate(responseParser.parse(json), parsed, log, ALLOWED);
    }

    private boolean hasError(DiagnosisValidator.Outcome outcome, String text) {
        return outcome.errors().stream().anyMatch(error -> error.contains(text));
    }

    @Test
    void acceptsAValidResponseAndUsesRealArticleTitles() {
        DiagnosisValidator.Outcome outcome = validate(AiTestData.validAiJson());

        assertTrue(outcome.valid());
        assertEquals(AnalysisMode.AI, outcome.response().analysisMode());
        assertEquals("PostgreSQL password authentication failed",
                outcome.response().knowledgeReferences().get(0).title());
    }

    @Test
    void rejectsAnInvalidCategory() {
        DiagnosisValidator.Outcome outcome = validate(
                AiTestData.validAiJson().replace("\"category\":\"DATABASE\"", "\"category\":\"BANANA\""));

        assertFalse(outcome.valid());
        assertTrue(hasError(outcome, "category must be one of"));
    }

    @Test
    void rejectsUnknownArticleIds() {
        DiagnosisValidator.Outcome outcome = validate(
                AiTestData.validAiJson().replace("\"articleId\":\"pg-auth-failed\"", "\"articleId\":\"made-up\""));

        assertTrue(hasError(outcome, "unknown articleId"));
    }

    @Test
    void rejectsEvidenceThatIsNotInTheLog() {
        DiagnosisValidator.Outcome outcome = validate(AiTestData.validAiJson().replace(
                "FATAL: password authentication failed for user fixflow_user",
                "Kubernetes pod OOMKilled by scheduler"));

        assertTrue(hasError(outcome, "does not match anything in the log"));
    }

    @Test
    void rejectsClaimsThatSomethingWasFixed() {
        DiagnosisValidator.Outcome outcome = validate(AiTestData.validAiJson().replace(
                "Update the application credentials to match the database role.",
                "I fixed the password for you."));

        assertTrue(hasError(outcome, "must not claim"));
    }

    @Test
    void rejectsHighConfidenceWhenTheLogHasNoEvidence() {
        DiagnosisValidator.Outcome outcome = validate(AiTestData.validAiJson(), "Something odd happened");

        assertTrue(hasError(outcome, "confidenceLevel HIGH"));
    }

    @Test
    void acceptsAnInsufficientEvidenceResponse() {
        DiagnosisValidator.Outcome outcome = validate(AiTestData.insufficientEvidenceJson(true), "Something odd happened");

        assertTrue(outcome.valid());
        assertTrue(outcome.response().needsMoreInformation());
    }

    @Test
    void rejectsInsufficientEvidenceWithoutFollowUpQuestions() {
        DiagnosisValidator.Outcome outcome = validate(AiTestData.insufficientEvidenceJson(false), "Something odd happened");

        assertTrue(hasError(outcome, "followUpQuestions"));
    }

    @Test
    void sortsAndRenumbersSteps() {
        String first = AiTestData.STEP_JSON.replace("\"order\":1", "\"order\":2")
                .replace("Compare the configured password with the database role password.", "First action");
        String second = AiTestData.STEP_JSON.replace("\"order\":1", "\"order\":5")
                .replace("Compare the configured password with the database role password.", "Second action");
        String json = AiTestData.validAiJson().replace(
                "\"recommendedSteps\":[" + AiTestData.STEP_JSON + "]",
                "\"recommendedSteps\":[" + second + "," + first + "]");

        DiagnosisValidator.Outcome outcome = validate(json);

        assertTrue(outcome.valid());
        assertEquals("First action", outcome.response().recommendedSteps().get(0).action());
        assertEquals(1, outcome.response().recommendedSteps().get(0).order());
        assertEquals(2, outcome.response().recommendedSteps().get(1).order());
    }
}