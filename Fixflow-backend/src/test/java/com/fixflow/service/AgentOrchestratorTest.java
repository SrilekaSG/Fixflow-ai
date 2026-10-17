package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.ai.AiPromptBuilder;
import com.fixflow.ai.AiResponseParser;
import com.fixflow.ai.AiTestData;
import com.fixflow.ai.StubAiProvider;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.DiagnosisStatus;
import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.AiProviderException.Reason;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentOrchestratorTest {

    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final StubAiProvider provider = new StubAiProvider();
    private final LogParserService parser = new LogParserService();
    private AgentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        when(knowledgeBaseService.findRelevant(any())).thenReturn(List.of(AiTestData.pgAuthArticle()));
        orchestrator = new AgentOrchestrator(knowledgeBaseService, new RuleBasedDiagnosisService(), provider,
                new AiPromptBuilder(), new AiResponseParser(new ObjectMapper()), new DiagnosisValidator());
    }

    private AgentOrchestrator.Result run() {
        return orchestrator.run(AiTestData.PG_AUTH_LOG, parser.parse(AiTestData.PG_AUTH_LOG));
    }

    @Test
    void unavailableProviderUsesRuleBasedAndNeverCallsTheAi() {
        provider.setAvailable(false);

        AgentOrchestrator.Result result = run();

        assertEquals(AnalysisMode.RULE_BASED, result.mode());
        assertEquals(DiagnosisStatus.COMPLETED, result.status());
        assertEquals(0, provider.callCount());
        assertEquals(AnalysisMode.RULE_BASED, result.response().analysisMode());
        assertTrue(result.response().analysisNote().contains("not configured"));
    }

    @Test
    void validAiResponseIsReturned() {
        provider.enqueue(AiTestData.validAiJson());

        AgentOrchestrator.Result result = run();

        assertEquals(AnalysisMode.AI, result.mode());
        assertEquals(DiagnosisStatus.COMPLETED, result.status());
        assertEquals("stub", result.providerName());
        assertEquals("stub-model", result.modelName());
        assertEquals(AnalysisMode.AI, result.response().analysisMode());
        assertEquals("PostgreSQL password authentication failed",
                result.response().knowledgeReferences().get(0).title());
        assertEquals(1, provider.callCount());
    }

    @Test
    void providerFailureFallsBackToTheRuleBasedResult() {
        provider.enqueueFailure(new AiProviderException(Reason.TIMEOUT, "The request timed out."));

        AgentOrchestrator.Result result = run();

        assertEquals(AnalysisMode.RULE_BASED, result.mode());
        assertEquals(DiagnosisStatus.PARTIAL, result.status());
        assertNull(result.providerName());
        assertEquals(AnalysisMode.RULE_BASED, result.response().analysisMode());
        assertTrue(result.response().analysisNote().contains("unavailable"));
        assertEquals("pg-auth-failed", result.response().knowledgeReferences().get(0).articleId());
    }

    @Test
    void invalidResponseIsRepairedOnce() {
        provider.enqueue("not json at all").enqueue(AiTestData.validAiJson());

        AgentOrchestrator.Result result = run();

        assertEquals(AnalysisMode.AI, result.mode());
        assertEquals(2, provider.callCount());
        assertTrue(provider.userPrompts().get(1).contains("rejected"));
    }

    @Test
    void responseThatStaysInvalidFallsBackToRuleBased() {
        provider.enqueue("not json").enqueue("still not json");

        AgentOrchestrator.Result result = run();

        assertEquals(AnalysisMode.RULE_BASED, result.mode());
        assertEquals(DiagnosisStatus.PARTIAL, result.status());
        assertEquals(2, provider.callCount());
        assertTrue(result.response().analysisNote().contains("invalid"));
    }
}