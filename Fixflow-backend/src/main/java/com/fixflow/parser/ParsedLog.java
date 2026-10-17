package com.fixflow.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ParsedLog(
        String timestamp,
        String exceptionClass,
        String errorMessage,
        String rootCauseClass,
        String rootCauseMessage,
        List<CauseEntry> causeChain,
        List<StackFrame> stackFrames,
        List<String> keyPatterns,
        List<String> relevantLines,
        int lineCount) {

    public record CauseEntry(String exceptionClass, String message) {
    }

    public static ParsedLog empty(int lineCount) {
        return new ParsedLog(null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), lineCount);
    }

    @JsonProperty("hasEvidence")
    public boolean hasEvidence() {
        return exceptionClass != null || !keyPatterns.isEmpty();
    }
}