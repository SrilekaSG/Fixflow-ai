package com.fixflow.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fixflow.service.KnowledgeBaseService;
import com.fixflow.service.LogInputService;
import com.fixflow.service.LogParserService;
import com.fixflow.service.RuleBasedDiagnosisService;
import com.fixflow.service.SecretRedactionService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fixflow.service.AnalysisService;

@WebMvcTest(DiagnosisController.class)
@Import({LogInputService.class, SecretRedactionService.class, LogParserService.class,
        RuleBasedDiagnosisService.class})
class DiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeBaseService knowledgeBaseService;
    
    @MockitoBean
    private AnalysisService analysisService;  

    @Test
    void previewRedactsSecretsAndParsesTheLog() throws Exception {
        String body = """
                {"content": "spring.datasource.password=SuperSecret123\\njava.lang.NullPointerException: boom"}
                """;

        mockMvc.perform(post("/api/v1/diagnoses/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("SuperSecret123"))))
                .andExpect(jsonPath("$.redactionCount").value(1))
                .andExpect(jsonPath("$.parsed.exceptionClass").value("java.lang.NullPointerException"))
                .andExpect(jsonPath("$.parsed.errorMessage").value("boom"))
                .andExpect(jsonPath("$.warning").exists());
    }

    @Test
    void blankContentReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unsupportedUploadReturns415() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/octet-stream",
                "abc".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/diagnoses/preview/upload").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void validUploadReturnsSanitizedPreview() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "app.log", "text/plain",
                "api_key=sk-abcdefghijklmnopqrstuvwxyz123456\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/diagnoses/preview/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sk-abcdefghijklmnopqrstuvwxyz123456"))))
                .andExpect(jsonPath("$.fileName").value("app.log"))
                .andExpect(jsonPath("$.redactionCount").value(1));
    }

    @Test
    void ruleBasedPreviewLabelsUnknownExceptionsHonestly() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses/rule-based-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"java.lang.ArithmeticException: / by zero\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisMode").value("RULE_BASED"))
                .andExpect(jsonPath("$.category").value("UNKNOWN"))
                .andExpect(jsonPath("$.needsMoreInformation").value(true));
    }

    @Test
    void ruleBasedPreviewReportsInsufficientEvidenceForVagueText() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses/rule-based-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"something odd happened\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value(startsWith("Insufficient evidence")))
                .andExpect(jsonPath("$.followUpQuestions.length()").value(3));
    }
}