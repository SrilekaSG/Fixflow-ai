package com.fixflow.controller;

import com.fixflow.dto.response.KnowledgeArticleResponse;
import com.fixflow.service.KnowledgeBaseService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    public List<KnowledgeArticleResponse> list(@RequestParam(name = "query", required = false) String query) {
        return knowledgeBaseService.search(query).stream().map(KnowledgeArticleResponse::from).toList();
    }
}