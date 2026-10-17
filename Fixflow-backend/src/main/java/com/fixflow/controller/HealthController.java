package com.fixflow.controller;

import com.fixflow.ai.AiProvider;
import com.fixflow.dto.response.HealthResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final AiProvider aiProvider;

    public HealthController(JdbcTemplate jdbcTemplate, AiProvider aiProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiProvider = aiProvider;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        boolean databaseUp = isDatabaseUp();
        HealthResponse body = new HealthResponse(
                databaseUp ? "UP" : "DEGRADED",
                databaseUp ? "UP" : "DOWN",
                aiProvider.isAvailable() ? "AI_CONFIGURED" : "RULE_BASED",
                Instant.now());
        return ResponseEntity.status(databaseUp ? 200 : 503).body(body);
    }

    private boolean isDatabaseUp() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}