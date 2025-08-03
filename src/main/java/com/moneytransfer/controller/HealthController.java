package com.moneytransfer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for Railway deployment.
 * Provides root-level endpoints for health checks and monitoring.
 */
@RestController
public class HealthController {

    /**
     * Simple ping endpoint for Railway health checks.
     * Accessible at: GET /ping
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * Root endpoint for basic connectivity test.
     * Accessible at: GET /
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Money Transfer Service is running");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("port", System.getProperty("server.port", "8080"));
        response.put("profile", System.getProperty("spring.profiles.active", "default"));
        response.put("env_profile", System.getenv("SPRING_PROFILES_ACTIVE"));
        response.put("jvm_profile", System.getProperty("spring.profiles.active"));
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for detailed status.
     * Accessible at: GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Money Transfer Service");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }
} 