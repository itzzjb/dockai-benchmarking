package com.example.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "API is running",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "API is running",
            "timestamp", Instant.now().toString()
        ));
    }
}
