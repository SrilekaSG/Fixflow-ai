package com.fixflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.Level;
import com.fixflow.entity.Severity;
import com.fixflow.service.AnalysisService;
import com.fixflow.service.KnowledgeBaseService;
import com.fixflow.service.LogInputService;
import com.fixflow.service.LogParserService;
import com.fixflow.service.RuleBasedDiagnosisService;
import com.fixflow.service.SecretRedactionService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(DiagnosisController.class)
class AnalyzeEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;
    @MockitoBean
    private LogInputService inputService;
    @MockitoBean
    private SecretRedactionService redactionService;
    @MockitoBean
    private LogParserService parserService;
    @MockitoBean
    private KnowledgeBaseService knowledgeBaseService;
    @MockitoBean
    private RuleBasedDiagnosisService ruleBasedDiagnosisService;

    private DiagnosisResponse sampleResponse() {
        return new DiagnosisResponse(UUID.randomUUID(), "Summary", ErrorCategory.DATABASE, Severity.HIGH, Level.HIGH,
                "Because.", List.of(), List.of(), "Fix.", List.of(), List.of(), List.of("Limit"), false, List.of(),
                AnalysisMode.RULE_BASED, "note");
    }

    @Test
    void postReturns201WithTheDiagnosis() throws Exception {
        when(analysisService.analyzePasted(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"some log\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary").value("Summary"))
                .andExpect(jsonPath("$.analysisMode").value("RULE_BASED"))
                .andExpect(jsonPath("$.diagnosisId").exists());
    }

    @Test
    void blankContentReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void uploadReturns201WithTheDiagnosis() throws Exception {
        when(analysisService.analyzeUpload(any(MultipartFile.class))).thenReturn(sampleResponse());
        MockMultipartFile file = new MockMultipartFile("file", "app.log", "text/plain",
                "some log".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/diagnoses/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary").value("Summary"));
    }
}