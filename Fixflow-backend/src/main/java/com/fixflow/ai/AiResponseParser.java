package com.fixflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.exception.InvalidAiResponseException;
import org.springframework.stereotype.Component;

@Component
public class AiResponseParser {

    static final int MAX_OUTPUT_CHARS = 20_000;

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiDiagnosisPayload parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidAiResponseException("The AI response was empty.");
        }
        if (raw.length() > MAX_OUTPUT_CHARS) {
            throw new InvalidAiResponseException("The AI response was too long.");
        }
        String json = extractJsonObject(raw);
        try {
            return objectMapper.readValue(json, AiDiagnosisPayload.class);
        } catch (JsonProcessingException e) {
            throw new InvalidAiResponseException("The AI response was not valid JSON for the expected format.");
        }
    }

    /** Tolerates markdown code fences or a short sentence around the JSON object. */
    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new InvalidAiResponseException("The AI response did not contain a JSON object.");
        }
        return raw.substring(start, end + 1);
    }
}