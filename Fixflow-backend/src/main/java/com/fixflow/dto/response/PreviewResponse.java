package com.fixflow.dto.response;

import com.fixflow.parser.ParsedLog;
import java.util.Map;

public record PreviewResponse(
        String sanitizedContent,
        String fileName,
        int redactionCount,
        Map<String, Integer> redactionsByType,
        String warning,
        ParsedLog parsed) {
}