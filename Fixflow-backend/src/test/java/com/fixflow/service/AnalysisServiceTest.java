package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.ai.AiPromptBuilder;
import com.fixflow.ai.AiResponseParser;
import com.fixflow.ai.AiTestData;
import com.fixflow.ai.StubAiProvider;
import com.fixflow.dto.request.AnalyzeLogRequest;
import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.Diagnosis;
import com.fixflow.entity.DiagnosisStatus;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.AiProviderException.Reason;
import com.fixflow.exception.InvalidLogException;
import com.fixflow.repository.DiagnosisRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class AnalysisServiceTest {

    private static final String SECRET = "SuperSecret123";

    private final StubAiProvider provider = new StubAiProvider();
    private final DiagnosisRepository repository = mock(DiagnosisRepository.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final UUID savedId = UUID.randomUUID();
    private AnalysisService service;

    @BeforeEach
    void setUp() {
        when(knowledgeBaseService.findRelevant(any())).thenReturn(List.of(AiTestData.pgAuthArticle()));
        when(repository.save(any(Diagnosis.class))).thenAnswer(invocation -> {
            Diagnosis diagnosis = invocation.getArgument(0);
            ReflectionTestUtils.setField(diagnosis, "id", savedId);
            return diagnosis;
        });
        AgentOrchestrator orchestrator = new AgentOrchestrator(knowledgeBaseService, new RuleBasedDiagnosisService(),
                provider, new AiPromptBuilder(), new AiResponseParser(new ObjectMapper()), new DiagnosisValidator());
        service = new AnalysisService(new LogInputService(1), new SecretRedactionService(), new LogParserService(),
                orchestrator, repository, new ObjectMapper());
    }

    private Diagnosis savedDiagnosis() {
        ArgumentCaptor<Diagnosis> captor = ArgumentCaptor.forClass(Diagnosis.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void secretsNeverReachTheAiProviderOrTheDatabase() {
        provider.enqueue(AiTestData.validAiJson());
        String content = "spring.datasource.password=" + SECRET + "\n" + AiTestData.PG_AUTH_LOG;

        DiagnosisResponse response = service.analyzePasted(new AnalyzeLogRequest(content, "My title", null));

        provider.systemPrompts().forEach(prompt -> assertFalse(prompt.contains(SECRET)));
        provider.userPrompts().forEach(prompt -> assertFalse(prompt.contains(SECRET)));
        Diagnosis saved = savedDiagnosis();
        assertFalse(saved.getSanitizedInput().contains(SECRET));
        assertTrue(saved.getSanitizedInput().contains("[REDACTED]"));
        assertFalse(saved.getDiagnosisJson().contains(SECRET));
        assertEquals(1, saved.getRedactionCount());
        assertEquals(savedId, response.diagnosisId());
        assertEquals(AnalysisMode.AI, saved.getAnalysisMode());
        assertEquals("stub", saved.getProviderName());
        assertEquals(DiagnosisStatus.COMPLETED, saved.getStatus());
        assertEquals("My title", saved.getTitle());
        assertEquals(ErrorCategory.DATABASE, saved.getCategory());
    }

    @Test
    void failedAiCallIsSavedAsAPartialRuleBasedResult() {
        provider.enqueueFailure(new AiProviderException(Reason.UNAVAILABLE, "The AI provider could not be reached."));

        DiagnosisResponse response = service.analyzePasted(new AnalyzeLogRequest(AiTestData.PG_AUTH_LOG, null, null));

        Diagnosis saved = savedDiagnosis();
        assertEquals(DiagnosisStatus.PARTIAL, saved.getStatus());
        assertEquals(AnalysisMode.RULE_BASED, saved.getAnalysisMode());
        assertNull(saved.getProviderName());
        assertEquals(AnalysisMode.RULE_BASED, response.analysisMode());
        assertEquals("PSQLException", saved.getTitle());
    }

    @Test
    void emptyInputIsRejectedBeforeAnythingElseHappens() {
        assertThrows(InvalidLogException.class,
                () -> service.analyzePasted(new AnalyzeLogRequest("   ", null, null)));

        verifyNoInteractions(repository);
        assertEquals(0, provider.callCount());
    }

    @Test
    void uploadedFilesUseTheSanitizedFileNameAsTitle() {
        provider.setAvailable(false);
        MockMultipartFile file = new MockMultipartFile("file", "..\\x\\app.log", "text/plain",
                AiTestData.PG_AUTH_LOG.getBytes(StandardCharsets.UTF_8));

        service.analyzeUpload(file);

        Diagnosis saved = savedDiagnosis();
        assertEquals("app.log", saved.getFileName());
        assertEquals("app.log", saved.getTitle());
        assertEquals(DiagnosisStatus.COMPLETED, saved.getStatus());
    }
}