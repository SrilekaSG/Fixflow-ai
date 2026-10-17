package com.fixflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.service.KnowledgeBaseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KnowledgeController.class)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void searchReturnsArticlesWithParsedLists() throws Exception {
        KnowledgeArticle article = new KnowledgeArticle("pg-auth-failed",
                "PostgreSQL password authentication failed", ErrorCategory.DATABASE,
                "password authentication failed", "Explanation.", "Cause one\nCause two",
                "Check the config | Confirms what is sent | LOW", "Connect with psql",
                "Update the credentials.", "Built-in FixFlow guide");
        when(knowledgeBaseService.search("postgres")).thenReturn(List.of(article));

        mockMvc.perform(get("/api/v1/knowledge").param("query", "postgres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pg-auth-failed"))
                .andExpect(jsonPath("$[0].category").value("DATABASE"))
                .andExpect(jsonPath("$[0].commonCauses.length()").value(2))
                .andExpect(jsonPath("$[0].diagnosticSteps[0].action").value("Check the config"))
                .andExpect(jsonPath("$[0].diagnosticSteps[0].risk").value("LOW"))
                .andExpect(jsonPath("$[0].sourceLabel").value("Built-in FixFlow guide"));
    }
}