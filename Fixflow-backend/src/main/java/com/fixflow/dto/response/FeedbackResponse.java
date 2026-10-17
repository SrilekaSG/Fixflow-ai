package com.fixflow.dto.response;

import com.fixflow.entity.FeedbackRating;
import java.util.UUID;

public record FeedbackResponse(UUID diagnosisId, FeedbackRating rating, String comment) {
}