package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fixflow.service.SecretRedactionService.RedactionResult;
import org.junit.jupiter.api.Test;

class SecretRedactionServiceTest {

    private final SecretRedactionService service = new SecretRedactionService();

    @Test
    void masksPasswordAssignments() {
        RedactionResult result = service.redact("spring.datasource.password=SuperSecret123\nserver.port=8080");

        assertFalse(result.sanitizedText().contains("SuperSecret123"));
        assertTrue(result.sanitizedText().contains("server.port=8080"));
        assertEquals(1, result.totalCount());
        assertEquals(1, (int) result.countsByType().get("SECRET_ASSIGNMENT"));
    }

    @Test
    void masksJsonStylePasswords() {
        RedactionResult result = service.redact("{\"password\": \"hunter2secret\", \"user\": \"bob\"}");

        assertFalse(result.sanitizedText().contains("hunter2secret"));
        assertTrue(result.sanitizedText().contains("bob"));
    }

    @Test
    void masksBearerTokensAndJwts() {
        String jwtSignature = "dBjftJeZ4CVPmB92K27uhbUJU1p1r";
        String input = "Authorization: Bearer abc123def456ghi789\n"
                + "token eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0." + jwtSignature;

        RedactionResult result = service.redact(input);

        assertFalse(result.sanitizedText().contains("abc123def456ghi789"));
        assertFalse(result.sanitizedText().contains(jwtSignature));
        assertTrue(result.sanitizedText().contains("Bearer"));
    }

    @Test
    void masksCredentialsInDatabaseUrls() {
        RedactionResult result = service.redact("url=jdbc:postgresql://admin:Pa55w0rd@db:5432/app");

        assertFalse(result.sanitizedText().contains("Pa55w0rd"));
        assertTrue(result.sanitizedText().contains("admin"));
        assertTrue(result.sanitizedText().contains("db:5432/app"));
    }

    @Test
    void masksApiKeysAndAwsKeys() {
        RedactionResult result = service.redact(
                "AWS key AKIAIOSFODNN7EXAMPLE and sk-abcdefghijklmnopqrstuvwxyz123456");

        assertFalse(result.sanitizedText().contains("AKIAIOSFODNN7EXAMPLE"));
        assertFalse(result.sanitizedText().contains("sk-abcdefghijklmnopqrstuvwxyz123456"));
        assertEquals(2, result.totalCount());
    }

    @Test
    void masksPrivateKeyBlocks() {
        String input = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA1234\n-----END RSA PRIVATE KEY-----";

        RedactionResult result = service.redact(input);

        assertFalse(result.sanitizedText().contains("MIIEowIBAAKCAQEA1234"));
        assertEquals(1, (int) result.countsByType().get("PRIVATE_KEY"));
    }

    @Test
    void doesNotCountAlreadyRedactedValuesTwice() {
        RedactionResult result = service.redact("api_key=sk-abcdefghijklmnopqrstuvwxyz123456");

        assertEquals(1, result.totalCount());
    }

    @Test
    void leavesOrdinaryLogsUntouched() {
        String input = "Exception in thread \"main\" java.lang.NullPointerException: Cannot invoke "
                + "\"String.length()\" because \"name\" is null\n"
                + "FATAL: password authentication failed for user \"fixflow_user\"";

        RedactionResult result = service.redact(input);

        assertEquals(input, result.sanitizedText());
        assertEquals(0, result.totalCount());
    }

    @Test
    void summaryContainsOnlyTypesAndCountsNeverSecretValues() {
        RedactionResult result = service.redact("db.password=TopSecretValue99");

        assertFalse(result.countsByType().toString().contains("TopSecretValue99"));
    }

    @Test
    void handlesNullAndEmptyInput() {
        assertEquals("", service.redact(null).sanitizedText());
        assertEquals(0, service.redact("").totalCount());
    }
}