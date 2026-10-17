package com.fixfloww;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.ai.AiTestData;
import com.fixflow.ai.StubAiProvider;
import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.AiProviderException.Reason;
import com.fixflow.repository.DiagnosisRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class FixFlowIntegrationTest {

    private static final String SECRET = "SuperSecret123";
    private static final String NPE_LOG = """
            Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
                at com.example.demo.UserService.greet(UserService.java:42)
                at com.example.demo.Main.main(Main.java:10)
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        StubAiProvider stubAiProvider() {
            return new StubAiProvider();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DiagnosisRepository diagnosisRepository;
    @Autowired
    private StubAiProvider stubAiProvider;

    @BeforeEach
    void setUp() {
        diagnosisRepository.deleteAll();
        stubAiProvider.reset().setAvailable(false);
    }

    private String json(String content) throws Exception {
        return objectMapper.writeValueAsString(Map.of("content", content));
    }

    private String analyze(String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.diagnosisId");
    }

    @Test
    void healthReportsDatabaseUpAndRuleBasedMode() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.analysisMode").value("RULE_BASED"));
    }

    @Test
    void migrationsSeedTheKnowledgeBase() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14));
    }

    @Test
    void knowledgeSearchFindsTheMatchingGuideFirst() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge").param("query", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pg-auth-failed"));
    }

    @Test
    void postgresAuthenticationErrorProducesAGroundedRuleBasedDiagnosis() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(AiTestData.PG_AUTH_LOG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosisId").exists())
                .andExpect(jsonPath("$.category").value("DATABASE"))
                .andExpect(jsonPath("$.analysisMode").value("RULE_BASED"))
                .andExpect(jsonPath("$.probableCauses[0].evidence", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.verificationSteps", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.knowledgeReferences[0].articleId").value("pg-auth-failed"));
    }

    @Test
    void nullPointerExceptionIsParsedAndClassifiedWithoutCrashing() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(NPE_LOG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("NULL_POINTER"))
                .andExpect(jsonPath("$.recommendedSteps[0].action", containsString("UserService.java:42")));
    }

    @Test
    void unknownLogsAreReportedAsUncertainNotGivenAFabricatedCause() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("it does not work")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("UNKNOWN"))
                .andExpect(jsonPath("$.needsMoreInformation").value(true))
                .andExpect(jsonPath("$.summary", startsWith("Insufficient evidence")))
                .andExpect(jsonPath("$.probableCauses", hasSize(0)));
    }

    @Test
    void secretsNeverReachTheAiProviderOrTheDatabase() throws Exception {
        stubAiProvider.setAvailable(true).enqueue(AiTestData.validAiJson());

        String id = analyze("spring.datasource.password=" + SECRET + "\n" + AiTestData.PG_AUTH_LOG);

        assertEquals(1, stubAiProvider.callCount());
        stubAiProvider.systemPrompts().forEach(prompt -> assertFalse(prompt.contains(SECRET)));
        stubAiProvider.userPrompts().forEach(prompt -> assertFalse(prompt.contains(SECRET)));
        mockMvc.perform(get("/api/v1/diagnoses/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(SECRET))))
                .andExpect(jsonPath("$.analysisMode").value("AI"))
                .andExpect(jsonPath("$.redactionCount").value(1));
        diagnosisRepository.findAll().forEach(saved -> {
            assertFalse(saved.getSanitizedInput().contains(SECRET));
            assertFalse(saved.getDiagnosisJson().contains(SECRET));
        });
    }

    @Test
    void aiFailureIsNeverPresentedAsASuccessfulAiDiagnosis() throws Exception {
        stubAiProvider.setAvailable(true)
                .enqueueFailure(new AiProviderException(Reason.TIMEOUT, "The AI provider did not respond in time."));

        String id = analyze(AiTestData.PG_AUTH_LOG);

        mockMvc.perform(get("/api/v1/diagnoses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.analysisMode").value("RULE_BASED"))
                .andExpect(jsonPath("$.diagnosis.analysisMode").value("RULE_BASED"))
                .andExpect(jsonPath("$.diagnosis.analysisNote", containsString("unavailable")));
    }

    @Test
    void emptyInputReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void oversizedUploadReturns413() throws Exception {
        MockMultipartFile big = new MockMultipartFile("file", "big.log", "text/plain", new byte[1024 * 1024 + 1]);

        mockMvc.perform(multipart("/api/v1/diagnoses/upload").file(big))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("INPUT_TOO_LARGE"));
    }

    @Test
    void unsupportedExtensionReturns415() throws Exception {
        MockMultipartFile exe = new MockMultipartFile("file", "app.exe", "application/octet-stream", "abc".getBytes());

        mockMvc.perform(multipart("/api/v1/diagnoses/upload").file(exe))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void invalidFeedbackReturns400() throws Exception {
        String id = analyze(AiTestData.PG_AUTH_LOG);

        mockMvc.perform(post("/api/v1/diagnoses/" + id + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"AMAZING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void missingDiagnosisReturns404() throws Exception {
        String missing = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/diagnoses/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/diagnoses/" + missing)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/diagnoses/" + missing + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"HELPFUL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyIsPaginatedAndADiagnosisCanBeOpenedAndDeleted() throws Exception {
        String first = analyze(NPE_LOG);
        analyze(AiTestData.PG_AUTH_LOG);
        analyze("it does not work");

        mockMvc.perform(get("/api/v1/diagnoses").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/diagnoses").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.items", hasSize(1)));

        mockMvc.perform(get("/api/v1/diagnoses/" + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis.category").value("NULL_POINTER"));
        mockMvc.perform(delete("/api/v1/diagnoses/" + first)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/diagnoses/" + first)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(jsonPath("$.totalDiagnoses").value(2));
    }

    @Test
    void feedbackIsPersistedAndShownAccurately() throws Exception {
        String id = analyze(AiTestData.PG_AUTH_LOG);

        mockMvc.perform(post("/api/v1/diagnoses/" + id + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"HELPFUL\",\"comment\":\"Worked well\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value("HELPFUL"));
        mockMvc.perform(get("/api/v1/diagnoses/" + id))
                .andExpect(jsonPath("$.feedbackRating").value("HELPFUL"))
                .andExpect(jsonPath("$.feedbackComment").value("Worked well"));
        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(jsonPath("$.helpfulCount").value(1))
                .andExpect(jsonPath("$.notHelpfulCount").value(0))
                .andExpect(jsonPath("$.totalDiagnoses").value(1));

        mockMvc.perform(post("/api/v1/diagnoses/" + id + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"NOT_HELPFUL\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(jsonPath("$.helpfulCount").value(0))
                .andExpect(jsonPath("$.notHelpfulCount").value(1));
    }
}