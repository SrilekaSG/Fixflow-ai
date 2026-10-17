package com.fixflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeLogRequest(
        @NotBlank(message = "content must not be empty") String content,
        @Size(max = 200, message = "title must be at most 200 characters") String title,
        @Size(max = 100, message = "sourceLabel must be at most 100 characters") String sourceLabel) {
}