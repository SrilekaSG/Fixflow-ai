package com.fixflow.dto.request;

import com.fixflow.entity.FeedbackRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull(message = "rating is required (HELPFUL or NOT_HELPFUL)") FeedbackRating rating,
        @Size(max = 500, message = "comment must be at most 500 characters") String comment) {
}