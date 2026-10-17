package com.fixflow.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fixflow.service.LogParserService;
import org.junit.jupiter.api.Test;

class LogParserServiceTest {

    private final LogParserService parser = new LogParserService();

    @Test
    void parsesNullPointerExceptionWithStackFrames() {
        String log = """
                2026-10-12 10:15:30.123 ERROR Something failed
                Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
                    at com.example.demo.UserService.greet(UserService.java:42)
                    at com.example.demo.Main.main(Main.java:10)
                """;

        ParsedLog parsed = parser.parse(log);

        assertEquals("2026-10-12 10:15:30.123", parsed.timestamp());
        assertEquals("java.lang.NullPointerException", parsed.exceptionClass());
        assertTrue(parsed.errorMessage().startsWith("Cannot invoke"));
        assertEquals(2, parsed.stackFrames().size());
        StackFrame first = parsed.stackFrames().get(0);
        assertEquals("com.example.demo.UserService", first.className());
        assertEquals("greet", first.methodName());
        assertEquals("UserService.java", first.fileName());
        assertEquals(42, first.lineNumber());
        assertTrue(first.applicationCode());
        assertTrue(parsed.keyPatterns().contains("NULL_POINTER"));
        assertTrue(parsed.hasEvidence());
    }

    @Test
    void parsesCauseChainAndRootCause() {
        String log = """
                2026-10-12T18:38:54.600+05:30 WARN Exception encountered during context initialization
                org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory'
                Caused by: org.flywaydb.core.internal.exception.FlywaySqlException: Unable to obtain connection from database
                Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed for user "fixflow_user"
                """;

        ParsedLog parsed = parser.parse(log);

        assertEquals("org.springframework.beans.factory.BeanCreationException", parsed.exceptionClass());
        assertEquals(2, parsed.causeChain().size());
        assertEquals("org.postgresql.util.PSQLException", parsed.rootCauseClass());
        assertTrue(parsed.rootCauseMessage().startsWith("FATAL: password authentication failed"));
        assertTrue(parsed.keyPatterns().contains("POSTGRES_AUTH_FAILED"));
        assertTrue(parsed.keyPatterns().contains("BEAN_CREATION_FAILED"));
    }

    @Test
    void marksFrameworkFramesAsNonApplicationCode() {
        String log = "java.lang.IllegalStateException: boom\n"
                + "    at org.springframework.boot.SpringApplication.run(SpringApplication.java:318)\n";

        ParsedLog parsed = parser.parse(log);

        assertFalse(parsed.stackFrames().get(0).applicationCode());
    }

    @Test
    void unknownLogsHaveNoEvidence() {
        ParsedLog parsed = parser.parse("Something odd happened but there are no clues here");

        assertNull(parsed.exceptionClass());
        assertTrue(parsed.keyPatterns().isEmpty());
        assertFalse(parsed.hasEvidence());
    }

    @Test
    void malformedAndEmptyInputDoesNotThrow() {
        assertDoesNotThrow(() -> parser.parse(null));
        assertDoesNotThrow(() -> parser.parse(""));
        ParsedLog parsed = assertDoesNotThrow(
                () -> parser.parse("\u0001 %%% Caused by: ::: at ((( ...\n at x.y("));

        assertFalse(parsed.hasEvidence());
    }
}