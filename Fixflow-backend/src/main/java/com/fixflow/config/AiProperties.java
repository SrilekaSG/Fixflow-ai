package com.fixflow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        @DefaultValue("disabled") String provider,
        @DefaultValue("") String apiKey,
        @DefaultValue("") String baseUrl,
        @DefaultValue("") String model,
        @DefaultValue("30s") Duration timeout,
        @DefaultValue("1500") int maxOutputTokens) {
}