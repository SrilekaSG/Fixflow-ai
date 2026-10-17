package com.fixflow.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.config.AiProperties;
import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.AiProviderException.Reason;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls any OpenAI-compatible chat completions API. Prompts, replies, and keys are never logged.
 */
@Component
public class ConfiguredLlmProvider implements AiProvider {

    static final String SUPPORTED_PROVIDER = "openai-compatible";
    private static final Logger log = LoggerFactory.getLogger(ConfiguredLlmProvider.class);

    private final AiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ConfiguredLlmProvider(AiProperties properties,
                                 @Qualifier("aiRestClient") RestClient restClient,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return SUPPORTED_PROVIDER.equalsIgnoreCase(strip(properties.provider()))
                && !strip(properties.baseUrl()).isEmpty()
                && !strip(properties.model()).isEmpty();
    }

    @Override
    public String providerName() {
        return isAvailable() ? SUPPORTED_PROVIDER : "disabled";
    }

    @Override
    public String modelName() {
        return strip(properties.model());
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new AiProviderException(Reason.NOT_CONFIGURED, "The AI provider is not configured.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        body.put("temperature", 0.2);
        body.put("max_tokens", properties.maxOutputTokens());

        URI endpoint;
        try {
            endpoint = URI.create(endpointUrl());
        } catch (IllegalArgumentException e) {
            throw new AiProviderException(Reason.NOT_CONFIGURED, "AI_BASE_URL is not a valid URL.");
        }

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        String key = strip(properties.apiKey());
                        if (!key.isEmpty()) {
                            headers.setBearerAuth(key);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            throw fromClientError(e.getStatusCode().value());
        } catch (HttpServerErrorException e) {
            log.warn("AI provider returned status {}", e.getStatusCode().value());
            throw new AiProviderException(Reason.UNAVAILABLE, "The AI provider returned a server error.");
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new AiProviderException(Reason.TIMEOUT, "The AI provider did not respond in time.");
            }
            throw new AiProviderException(Reason.UNAVAILABLE, "The AI provider could not be reached.");
        } catch (RestClientException e) {
            throw new AiProviderException(Reason.UNAVAILABLE, "The AI provider call failed.");
        }
        return extractContent(responseBody);
    }

    private AiProviderException fromClientError(int status) {
        log.warn("AI provider rejected the request with status {}", status);
        if (status == 401 || status == 403) {
            return new AiProviderException(Reason.AUTHENTICATION,
                    "The AI provider rejected the credentials (check AI_API_KEY).");
        }
        if (status == 429) {
            return new AiProviderException(Reason.RATE_LIMITED,
                    "The AI provider rate limit or quota was reached.");
        }
        return new AiProviderException(Reason.REJECTED,
                "The AI provider rejected the request (check AI_BASE_URL, AI_MODEL, and your quota).");
    }

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AiProviderException(Reason.BAD_RESPONSE, "The AI provider returned an empty response.");
        }
        try {
            JsonNode content = objectMapper.readTree(responseBody).path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
                throw new AiProviderException(Reason.BAD_RESPONSE, "The AI provider returned no message content.");
            }
            return content.asText();
        } catch (JsonProcessingException e) {
            throw new AiProviderException(Reason.BAD_RESPONSE, "The AI provider returned an unreadable response.");
        }
    }

    private String endpointUrl() {
        String base = strip(properties.baseUrl());
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/chat/completions";
    }

    private String strip(String value) {
        return value == null ? "" : value.strip();
    }
}