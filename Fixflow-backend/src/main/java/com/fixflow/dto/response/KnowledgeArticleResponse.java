package com.fixflow.dto.response;

import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.service.ArticleText;
import com.fixflow.service.ArticleText.StepText;
import java.util.List;

public record KnowledgeArticleResponse(
        String id,
        String title,
        ErrorCategory category,
        String keywords,
        String explanation,
        List<String> commonCauses,
        List<StepText> diagnosticSteps,
        List<String> verificationSteps,
        String suggestedFix,
        String sourceLabel) {

    public static KnowledgeArticleResponse from(KnowledgeArticle article) {
        return new KnowledgeArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getCategory(),
                article.getKeywords(),
                article.getBody(),
                ArticleText.lines(article.getCommonCauses()),
                ArticleText.steps(article.getDiagnosticSteps()),
                ArticleText.lines(article.getVerificationSteps()),
                article.getSuggestedFix(),
                article.getSourceLabel());
    }
}