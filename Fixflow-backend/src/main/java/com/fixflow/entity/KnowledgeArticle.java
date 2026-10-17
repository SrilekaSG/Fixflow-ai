package com.fixflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_articles")
public class KnowledgeArticle {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ErrorCategory category;

    @Column(name = "keywords")
    private String keywords;

    @Column(name = "body")
    private String body;

    @Column(name = "common_causes")
    private String commonCauses;

    @Column(name = "diagnostic_steps")
    private String diagnosticSteps;

    @Column(name = "verification_steps")
    private String verificationSteps;

    @Column(name = "suggested_fix")
    private String suggestedFix;

    @Column(name = "source_label")
    private String sourceLabel;

    protected KnowledgeArticle() {
    }

    public KnowledgeArticle(String id, String title, ErrorCategory category, String keywords, String body,
                            String commonCauses, String diagnosticSteps, String verificationSteps,
                            String suggestedFix, String sourceLabel) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.keywords = keywords;
        this.body = body;
        this.commonCauses = commonCauses;
        this.diagnosticSteps = diagnosticSteps;
        this.verificationSteps = verificationSteps;
        this.suggestedFix = suggestedFix;
        this.sourceLabel = sourceLabel;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getBody() {
        return body;
    }

    public String getCommonCauses() {
        return commonCauses;
    }

    public String getDiagnosticSteps() {
        return diagnosticSteps;
    }

    public String getVerificationSteps() {
        return verificationSteps;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }
}