package com.fixflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "diagnoses")
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "title")
    private String title;

    @Column(name = "sanitized_input", nullable = false)
    private String sanitizedInput;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "exception_class")
    private String exceptionClass;

    @Column(name = "error_message")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ErrorCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "diagnosis_json")
    private String diagnosisJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_mode", nullable = false)
    private AnalysisMode analysisMode;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "model_name")
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DiagnosisStatus status;

    @Column(name = "redaction_count", nullable = false)
    private int redactionCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_rating")
    private FeedbackRating feedbackRating;

    @Column(name = "feedback_comment")
    private String feedbackComment;

    @ManyToMany
    @JoinTable(name = "diagnosis_matched_articles",
            joinColumns = @JoinColumn(name = "diagnosis_id"),
            inverseJoinColumns = @JoinColumn(name = "article_id"))
    private Set<KnowledgeArticle> matchedArticles = new LinkedHashSet<>();

    protected Diagnosis() {
    }

    public Diagnosis(String title, String sanitizedInput, String fileName, String exceptionClass,
                     String errorMessage, ErrorCategory category, Severity severity, String diagnosisJson,
                     AnalysisMode analysisMode, String providerName, String modelName, DiagnosisStatus status,
                     int redactionCount, Set<KnowledgeArticle> matchedArticles) {
        this.title = title;
        this.sanitizedInput = sanitizedInput;
        this.fileName = fileName;
        this.exceptionClass = exceptionClass;
        this.errorMessage = errorMessage;
        this.category = category;
        this.severity = severity;
        this.diagnosisJson = diagnosisJson;
        this.analysisMode = analysisMode;
        this.providerName = providerName;
        this.modelName = modelName;
        this.status = status;
        this.redactionCount = redactionCount;
        this.matchedArticles = new LinkedHashSet<>(matchedArticles);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public String getSanitizedInput() {
        return sanitizedInput;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDiagnosisJson() {
        return diagnosisJson;
    }

    public AnalysisMode getAnalysisMode() {
        return analysisMode;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getModelName() {
        return modelName;
    }

    public DiagnosisStatus getStatus() {
        return status;
    }

    public int getRedactionCount() {
        return redactionCount;
    }

    public FeedbackRating getFeedbackRating() {
        return feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public void setFeedback(FeedbackRating rating, String comment) {
        this.feedbackRating = rating;
        this.feedbackComment = comment;
    }

    public Set<KnowledgeArticle> getMatchedArticles() {
        return matchedArticles;
    }
}