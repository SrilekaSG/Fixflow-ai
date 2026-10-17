package com.fixflow.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.exception.InvalidAiResponseException;
import org.junit.jupiter.api.Test;

class AiResponseParserTest {

    private final AiResponseParser parser = new AiResponseParser(new ObjectMapper());

    @Test
    void parsesPlainJson() {
        AiDiagnosisPayload payload = parser.parse(AiTestData.validAiJson());

        assertEquals("DATABASE", payload.category());
        assertEquals(1, payload.probableCauses().size());
        assertEquals("pg-auth-failed", payload.knowledgeReferences().get(0).articleId());
    }

    @Test
    void toleratesCodeFencesAndSurroundingText() {
        String raw = "Here is the result:\n```json\n" + AiTestData.validAiJson() + "\n```";

        assertEquals("DATABASE", parser.parse(raw).category());
    }

    @Test
    void ignoresUnknownFields() {
        String raw = AiTestData.validAiJson().replaceFirst("\\{", "{\"extra\":1,");

        assertEquals("HIGH", parser.parse(raw).severity());
    }

    @Test
    void rejectsEmptyAndNonJsonResponses() {
        assertThrows(InvalidAiResponseException.class, () -> parser.parse(null));
        assertThrows(InvalidAiResponseException.class, () -> parser.parse("   "));
        assertThrows(InvalidAiResponseException.class, () -> parser.parse("I cannot help with that."));
    }

    @Test
    void rejectsJsonWithTheWrongShape() {
        assertThrows(InvalidAiResponseException.class, () -> parser.parse("{\"summary\": [\"a\"]}"));
    }

    @Test
    void rejectsOversizedResponses() {
        String huge = "{" + "a".repeat(30_000) + "}";

        assertThrows(InvalidAiResponseException.class, () -> parser.parse(huge));
    }
}