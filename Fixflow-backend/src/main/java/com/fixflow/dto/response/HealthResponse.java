package com.fixflow.dto.response;

import java.time.Instant;

public record HealthResponse(
        String status,
        String database,
        String analysisMode,
        Instant timestamp) {
}