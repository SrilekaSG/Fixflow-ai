package com.fixflow.service;

import com.fixflow.entity.Risk;
import java.util.Arrays;
import java.util.List;

/** Splits the multi-line text columns of a knowledge article into lists. */
public final class ArticleText {

    public record StepText(String action, String reason, Risk risk) {
    }

    private ArticleText() {
    }

    public static List<String> lines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\R"))
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    /** Each step line has the form: action | reason | RISK. */
    public static List<StepText> steps(String text) {
        return lines(text).stream().map(ArticleText::toStep).toList();
    }

    private static StepText toStep(String line) {
        String[] parts = line.split("\\|", 3);
        String action = parts[0].strip();
        String reason = parts.length > 1 ? parts[1].strip() : "Helps narrow down the cause.";
        Risk risk = parts.length > 2 ? parseRisk(parts[2].strip()) : Risk.LOW;
        return new StepText(action, reason, risk);
    }

    private static Risk parseRisk(String value) {
        try {
            return Risk.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Risk.LOW;
        }
    }
}