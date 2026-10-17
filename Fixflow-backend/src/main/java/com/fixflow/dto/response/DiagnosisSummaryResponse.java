package com.fixflow.dto.response;

import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.Diagnosis;
import com.fixflow.entity.DiagnosisStatus;
import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.FeedbackRating;
import com.fixflow.entity.Severity;
import java.time.Instant;
import java.util.UUID;

public record DiagnosisSummaryResponse(
        UUID id,
        Instant createdAt,
        String title,
        String exceptionClass,
        ErrorCategory category,
        Severity severity,
        DiagnosisStatus status,
        AnalysisMode analysisMode,
        FeedbackRating feedbackRating) {

    public static DiagnosisSummaryResponse from(Diagnosis diagnosis) {
        return new DiagnosisSummaryResponse(
                diagnosis.getId(),
                diagnosis.getCreatedAt(),
                diagnosis.getTitle(),
                diagnosis.getExceptionClass(),
                diagnosis.getCategory(),
                diagnosis.getSeverity(),
                diagnosis.getStatus(),
                diagnosis.getAnalysisMode(),
                diagnosis.getFeedbackRating());
    }
}