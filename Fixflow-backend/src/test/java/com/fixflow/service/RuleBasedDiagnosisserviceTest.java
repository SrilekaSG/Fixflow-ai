package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.entity.Level;
import com.fixflow.entity.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedDiagnosisServiceTest {

    private final LogParserService parser = new LogParserService();
    private final RuleBasedDiagnosisService service = new RuleBasedDiagnosisService();

    private KnowledgeArticle article(String id, String title, ErrorCategory category) {
        return new KnowledgeArticle(id, title, category, "keywords", "Explanation text.",
                "First common cause\nSecond common cause",
                "Check the configuration | Confirms what the app sends | LOW\nTry a manual login | Separates problems | LOW",
                "Confirm the connection works\nRestart the application",
                "Update the configuration.", "Built-in FixFlow guide");
    }

    @Test
    void knownDistinctiveSignatureGivesHighConfidenceDiagnosis() {
        String log = "Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed for user \"x\"";
        KnowledgeArticle guide = article("pg-auth-failed", "PostgreSQL password authentication failed", ErrorCategory.DATABASE);

        DiagnosisResponse response = service.diagnose(parser.parse(log), List.of(guide));

        assertEquals(ErrorCategory.DATABASE, response.category());
        assertEquals(Severity.HIGH, response.severity());
        assertEquals(Level.HIGH, response.confidenceLevel());
        assertEquals(AnalysisMode.RULE_BASED, response.analysisMode());
        assertFalse(response.needsMoreInformation());
        assertEquals(2, response.probableCauses().size());
        assertFalse(response.probableCauses().get(0).evidence().isEmpty());
        assertEquals(Level.HIGH, response.probableCauses().get(0).likelihood());
        assertEquals(Level.LOW, response.probableCauses().get(1).likelihood());
        assertEquals(2, response.recommendedSteps().size());
        assertEquals(1, response.recommendedSteps().get(0).order());
        assertEquals(2, response.recommendedSteps().get(1).order());
        assertEquals(2, response.verificationSteps().size());
        assertEquals("pg-auth-failed", response.knowledgeReferences().get(0).articleId());
        assertTrue(response.suggestedFix().contains("has not changed or run anything"));
        assertTrue(response.analysisNote().contains("no AI model"));
    }

    @Test
    void mostSpecificSignatureBecomesPrimaryAndOthersAreMentioned() {
        String log = """
                org.springframework.beans.factory.BeanCreationException: Error creating bean
                Caused by: java.net.ConnectException: Connection refused: getsockopt
                """;
        KnowledgeArticle connection = article("db-connection-refused", "Connection refused to database", ErrorCategory.DATABASE);
        KnowledgeArticle bean = article("spring-bean-creation-failure", "Bean creation failure", ErrorCategory.DEPENDENCY);

        DiagnosisResponse response = service.diagnose(parser.parse(log), List.of(connection, bean));

        assertEquals(ErrorCategory.DATABASE, response.category());
        assertEquals(Level.MODERATE, response.confidenceLevel());
        assertEquals(2, response.knowledgeReferences().size());
        assertTrue(response.limitations().stream().anyMatch(text -> text.contains("Other error signatures")));
    }

    @Test
    void vagueTextReportsInsufficientEvidence() {
        DiagnosisResponse response = service.diagnose(parser.parse("Something odd happened"), List.of());

        assertEquals(ErrorCategory.UNKNOWN, response.category());
        assertEquals(Severity.UNKNOWN, response.severity());
        assertEquals(Level.LOW, response.confidenceLevel());
        assertTrue(response.needsMoreInformation());
        assertFalse(response.followUpQuestions().isEmpty());
        assertTrue(response.summary().startsWith("Insufficient evidence"));
        assertTrue(response.probableCauses().isEmpty());
        assertEquals(AnalysisMode.RULE_BASED, response.analysisMode());
    }

    @Test
    void unknownExceptionIsNotGivenAFabricatedCause() {
        String log = """
                java.lang.ArithmeticException: / by zero
                    at com.example.Calc.divide(Calc.java:12)
                """;

        DiagnosisResponse response = service.diagnose(parser.parse(log), List.of());

        assertEquals(ErrorCategory.UNKNOWN, response.category());
        assertEquals(Level.LOW, response.confidenceLevel());
        assertTrue(response.needsMoreInformation());
        assertTrue(response.recommendedSteps().get(0).action().contains("Calc.java:12"));
        assertTrue(response.probableCauses().get(0).evidence().get(0).startsWith("Exception: java.lang.ArithmeticException"));
    }

    @Test
    void nullPointerDiagnosisStartsWithTheApplicationFrame() {
        String log = """
                java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
                    at com.example.demo.UserService.greet(UserService.java:42)
                """;
        KnowledgeArticle guide = article("java-null-pointer", "Java NullPointerException", ErrorCategory.NULL_POINTER);

        DiagnosisResponse response = service.diagnose(parser.parse(log), List.of(guide));

        assertEquals(ErrorCategory.NULL_POINTER, response.category());
        assertEquals(3, response.recommendedSteps().size());
        assertEquals(1, response.recommendedSteps().get(0).order());
        assertTrue(response.recommendedSteps().get(0).action().contains("UserService.java:42"));
    }
}