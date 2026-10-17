package com.fixflow.dto.response;

import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.DiagnosisStatus;
import com.fixflow.entity.FeedbackRating;
import java.time.Instant;
import java.util.UUID;

public record DiagnosisDetailResponse(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        String title,
        String fileName,
        String sanitizedInput,
        int redactionCount,
        DiagnosisStatus status,
        AnalysisMode analysisMode,
        String providerName,
        String modelName,
        FeedbackRating feedbackRating,
        String feedbackComment,
        DiagnosisResponse diagnosis) {
}