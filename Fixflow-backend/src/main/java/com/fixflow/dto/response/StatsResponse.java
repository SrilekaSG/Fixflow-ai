package com.fixflow.dto.response;

public record StatsResponse(
        long totalDiagnoses,
        long helpfulCount,
        long notHelpfulCount,
        long aiAnalysisCount,
        long ruleBasedCount) {
}