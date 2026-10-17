package com.fixflow.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.config.AiProperties;
import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.AiProviderException.Reason;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ConfiguredLlmProviderTest {

    private static final String URL = "https://llm.example.test/v1/chat/completions";

    private MockRestServiceServer server;

    private ConfiguredLlmProvider provider(String providerName) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AiProperties properties = new AiProperties(providerName, "test-key", "https://llm.example.test/v1/",
                "test-model", Duration.ofSeconds(5), 500);
        return new ConfiguredLlmProvider(properties, builder.build(), new ObjectMapper());
    }

    private Reason failureReason(ConfiguredLlmProvider provider) {
        return assertThrows(AiProviderException.class, () -> provider.generate("system", "user")).getReason();
    }

    @Test
    void returnsTheMessageContentAndSendsTheExpectedRequest() {
        ConfiguredLlmProvider provider = provider("openai-compatible");
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertEquals("hello", provider.generate("system", "user"));
        server.verify();
    }

    @Test
    void mapsAuthenticationFailures() {
        ConfiguredLlmProvider provider = provider("openai-compatible");
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertEquals(Reason.AUTHENTICATION, failureReason(provider));
    }

    @Test
    void mapsRateLimits() {
        ConfiguredLlmProvider provider = provider("openai-compatible");
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertEquals(Reason.RATE_LIMITED, failureReason(provider));
    }

    @Test
    void mapsServerErrors() {
        ConfiguredLlmProvider provider = provider("openai-compatible");
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertEquals(Reason.UNAVAILABLE, failureReason(provider));
    }

    @Test
    void mapsTimeouts() {
        ConfiguredLlmProvider provider = provider("openai-compatible");
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("timeout")));

        assertEquals(Reason.TIMEOUT, failureReason(provider));
    }

    @Test
    void rejectsResponsesWithoutContent() {
        ConfiguredLlmProvider provider = provider("openai-compatible");
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertEquals(Reason.BAD_RESPONSE, failureReason(provider));
    }

    @Test
    void disabledProviderIsNotAvailableAndMakesNoCall() {
        ConfiguredLlmProvider provider = provider("disabled");

        assertFalse(provider.isAvailable());
        assertEquals(Reason.NOT_CONFIGURED, failureReason(provider));
        server.verify();
    }
}