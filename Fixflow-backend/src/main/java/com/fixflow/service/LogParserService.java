package com.fixflow.service;

import com.fixflow.parser.ParsedLog;
import com.fixflow.parser.ParsedLog.CauseEntry;
import com.fixflow.parser.StackFrame;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Defensive, regex-based parser for Java / Spring Boot logs. It must only be given sanitized text.
 */
@Service
public class LogParserService {

    private static final Logger log = LoggerFactory.getLogger(LogParserService.class);

    private record KeyPattern(String id, Pattern pattern) {
    }

    private static final int MAX_LINE_LENGTH = 2000;
    private static final int MAX_FRAMES = 50;
    private static final int MAX_RELEVANT_LINES = 5;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_RELEVANT_LINE_LENGTH = 300;
    private static final String CAUSED_BY = "Caused by:";

    private static final Pattern TIMESTAMP = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)");
    private static final Pattern FRAME = Pattern.compile(
            "^\\s*at\\s+([\\w$<>./-]+)\\.([\\w$<>]+)\\(([^():]*)(?::(\\d+))?\\)");
    private static final Pattern EXCEPTION = Pattern.compile(
            "\\b((?:[a-z]\\w*\\.)+(?:[A-Z][\\w$]*)?(?:Exception|Error))\\b(?::\\s*(.*))?");
    private static final Pattern RELEVANT_LINE = Pattern.compile("\\b(?:ERROR|FATAL|SEVERE)\\b|Exception");

    private static final List<String> FRAMEWORK_PREFIXES = List.of(
            "java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun.", "org.springframework.", "org.apache.",
            "org.hibernate.", "org.postgresql.", "org.flywaydb.", "com.zaxxer.", "org.junit.", "org.eclipse.",
            "io.netty.", "reactor.");

    private static final List<KeyPattern> KEY_PATTERNS = List.of(
            new KeyPattern("POSTGRES_AUTH_FAILED", ci("password authentication failed")),
            new KeyPattern("CONNECTION_REFUSED", ci("connection[^\\n]{0,80}refused")),
            new KeyPattern("UNKNOWN_DATABASE",
                    ci("database \"[^\"]+\" does not exist|unknown database|schema \"[^\"]+\" does not exist")),
            new KeyPattern("NULL_POINTER", Pattern.compile("NullPointerException")),
            new KeyPattern("DATASOURCE_NOT_CONFIGURED",
                    ci("failed to configure a datasource|cannot determine embedded database driver")),
            new KeyPattern("NO_QUALIFYING_BEAN", ci("NoSuchBeanDefinitionException|No qualifying bean")),
            new KeyPattern("BEAN_CREATION_FAILED",
                    Pattern.compile("BeanCreationException|UnsatisfiedDependencyException|BeanInstantiationException")),
            new KeyPattern("PORT_IN_USE",
                    ci("port \\d+ (?:was|is) already in use|Address already in use|BindException")),
            new KeyPattern("HTTP_404",
                    ci("\\bstatus[=: ]+404\\b|\\b404 not found\\b|NoHandlerFoundException|NoResourceFoundException")),
            new KeyPattern("HTTP_500",
                    ci("\\bstatus[=: ]+500\\b|\\b500 internal server error\\b|HttpServerErrorException")),
            new KeyPattern("UNRESOLVED_PLACEHOLDER",
                    ci("Could not resolve placeholder|Cannot resolve placeholder")),
            new KeyPattern("MAVEN_BUILD_FAILURE",
                    ci("BUILD FAILURE|Could not resolve dependencies|Failed to execute goal|Could not transfer artifact")),
            new KeyPattern("CLASS_NOT_FOUND", Pattern.compile("ClassNotFoundException|NoClassDefFoundError")),
            new KeyPattern("JSON_OR_VALIDATION",
                    ci("HttpMessageNotReadableException|JsonParseException|MethodArgumentNotValidException"
                            + "|ConstraintViolationException|JSON parse error")));

    public ParsedLog parse(String sanitizedText) {
        if (sanitizedText == null || sanitizedText.isBlank()) {
            return ParsedLog.empty(0);
        }
        try {
            return doParse(sanitizedText);
        } catch (RuntimeException e) {
            log.warn("Log parsing failed unexpectedly: {}", e.getClass().getSimpleName());
            return ParsedLog.empty(countLines(sanitizedText));
        }
    }

    private ParsedLog doParse(String text) {
        String[] lines = text.split("\n", -1);
        String timestamp = null;
        CauseEntry primary = null;
        List<CauseEntry> causes = new ArrayList<>();
        List<StackFrame> frames = new ArrayList<>();
        List<String> relevant = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.length() > MAX_LINE_LENGTH ? rawLine.substring(0, MAX_LINE_LENGTH) : rawLine;
            String trimmed = line.strip();

            if (timestamp == null) {
                Matcher timeMatcher = TIMESTAMP.matcher(line);
                if (timeMatcher.find()) {
                    timestamp = timeMatcher.group(1);
                }
            }

            Matcher frameMatcher = FRAME.matcher(line);
            if (frameMatcher.find()) {
                if (frames.size() < MAX_FRAMES) {
                    frames.add(toFrame(frameMatcher));
                }
                continue;
            }

            boolean causedBy = trimmed.startsWith(CAUSED_BY);
            Matcher exceptionMatcher = EXCEPTION.matcher(causedBy ? trimmed.substring(CAUSED_BY.length()) : line);
            if (exceptionMatcher.find()) {
                CauseEntry entry = new CauseEntry(exceptionMatcher.group(1), cleanMessage(exceptionMatcher.group(2)));
                if (causedBy) {
                    causes.add(entry);
                } else if (primary == null) {
                    primary = entry;
                }
            }

            if (relevant.size() < MAX_RELEVANT_LINES && RELEVANT_LINE.matcher(line).find()) {
                relevant.add(truncate(trimmed, MAX_RELEVANT_LINE_LENGTH));
            }
        }

        if (primary == null && !causes.isEmpty()) {
            primary = causes.get(0);
        }
        CauseEntry root = causes.isEmpty() ? primary : causes.get(causes.size() - 1);

        return new ParsedLog(
                timestamp,
                primary == null ? null : primary.exceptionClass(),
                primary == null ? null : primary.message(),
                root == null ? null : root.exceptionClass(),
                root == null ? null : root.message(),
                List.copyOf(causes),
                List.copyOf(frames),
                findKeyPatterns(text),
                List.copyOf(relevant),
                lines.length);
    }

    private StackFrame toFrame(Matcher matcher) {
        String className = matcher.group(1);
        String methodName = matcher.group(2);
        String fileName = matcher.group(3) == null || matcher.group(3).isBlank() ? null : matcher.group(3).strip();
        return new StackFrame(className, methodName, fileName, parseLineNumber(matcher.group(4)),
                isApplicationCode(className));
    }

    private Integer parseLineNumber(String value) {
        if (value == null || value.length() > 9) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private boolean isApplicationCode(String className) {
        String name = className;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        for (String prefix : FRAMEWORK_PREFIXES) {
            if (name.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private List<String> findKeyPatterns(String text) {
        Set<String> found = new LinkedHashSet<>();
        for (KeyPattern keyPattern : KEY_PATTERNS) {
            if (keyPattern.pattern().matcher(text).find()) {
                found.add(keyPattern.id());
            }
        }
        return List.copyOf(found);
    }

    private String cleanMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return truncate(message.strip(), MAX_MESSAGE_LENGTH);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private int countLines(String text) {
        return text.split("\n", -1).length;
    }

    private static Pattern ci(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }
}