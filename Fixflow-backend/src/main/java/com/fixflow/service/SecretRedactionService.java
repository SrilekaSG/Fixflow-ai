package com.fixflow.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Masks likely secrets using regular expressions. This is heuristic: it will miss some secrets
 * and can mask harmless text, which is why the UI shows the sanitized preview before analysis.
 */
@Service
public class SecretRedactionService {

    public record RedactionResult(String sanitizedText, int totalCount, Map<String, Integer> countsByType) {
    }

    private record Rule(String type, Pattern pattern, String replacement) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("PRIVATE_KEY",
                    Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
                    "[REDACTED_PRIVATE_KEY]"),
            new Rule("JWT",
                    Pattern.compile("\\beyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}"),
                    "[REDACTED_JWT]"),
            new Rule("API_KEY",
                    Pattern.compile("\\b(?:AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{20,}"
                            + "|sk-[A-Za-z0-9_\\-]{20,}|AIza[0-9A-Za-z_\\-]{35})"),
                    "[REDACTED_API_KEY]"),
            new Rule("DB_URL_CREDENTIALS",
                    Pattern.compile("(?i)\\b([a-z][a-z0-9+.\\-]*://[^/\\s:@]+:)[^/\\s@]+@"),
                    "$1[REDACTED]@"),
            new Rule("BEARER_TOKEN",
                    Pattern.compile("(?i)\\b(Bearer)\\s+(?=[A-Za-z0-9\\-._~+/]*[0-9.])[A-Za-z0-9\\-._~+/]{8,}=*"),
                    "$1 [REDACTED_TOKEN]"),
            new Rule("BASIC_AUTH",
                    Pattern.compile("(?i)\\b(Authorization\\s*:\\s*Basic)\\s+[A-Za-z0-9+/]{8,}=*"),
                    "$1 [REDACTED_TOKEN]"),
            new Rule("SECRET_ASSIGNMENT",
                    Pattern.compile("(?i)([A-Za-z0-9_.\\-]{0,60}"
                            + "(?:password|passwd|pwd|secret|token|api[_-]?key|access[_-]?key|private[_-]?key|credentials?)"
                            + "[A-Za-z0-9_.\\-]{0,60}[\"']?\\s*[:=]\\s*)"
                            + "(?!\\[REDACTED)(\"[^\"\\n]*\"|'[^'\\n]*'|[^\\s,;&\"'}\\]]+)"),
                    "$1[REDACTED]"));

    public RedactionResult redact(String text) {
        if (text == null || text.isEmpty()) {
            return new RedactionResult("", 0, Map.of());
        }
        String current = text;
        int total = 0;
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(current);
            StringBuilder builder = new StringBuilder();
            int count = 0;
            while (matcher.find()) {
                count++;
                matcher.appendReplacement(builder, rule.replacement());
            }
            if (count > 0) {
                matcher.appendTail(builder);
                current = builder.toString();
                total += count;
                byType.merge(rule.type(), count, Integer::sum);
            }
        }
        return new RedactionResult(current, total, Collections.unmodifiableMap(byType));
    }
}