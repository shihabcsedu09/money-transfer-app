package com.moneytransfer.controller;

import com.moneytransfer.dto.TransferRequest;
import com.moneytransfer.dto.TransferResponse;
import com.moneytransfer.exception.InsufficientFundsException;
import com.moneytransfer.exception.TransferException;
import com.moneytransfer.service.TransferService;
import io.micrometer.core.annotation.Timed;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for money transfer operations.
 * 
 * Features:
 * - Comprehensive error handling
 * - Request validation
 * - Metrics and monitoring
 * - Proper HTTP status codes
 */
@RestController
@RequestMapping("/api/v1/transfers")
@Timed(value = "transfer.operations", description = "Time taken for transfer operations")
public class TransferController {

    private static final Logger logger = LoggerFactory.getLogger(TransferController.class);

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Process a money transfer.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> processTransfer(@Valid @RequestBody TransferRequest request) {
        logger.info("Received transfer request: {}", request);
        
        try {
            TransferResponse response = transferService.processTransfer(request);
            logger.info("Transfer processed successfully: {}", response.getTransferId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InsufficientFundsException e) {
            logger.warn("Insufficient funds for transfer: {}", e.getMessage());
            throw e;
        } catch (TransferException e) {
            logger.error("Transfer processing failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get transfer details by transfer ID.
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String transferId) {
        logger.info("Retrieving transfer details for: {}", transferId);
        
        try {
            TransferResponse response = transferService.getTransfer(transferId);
            return ResponseEntity.ok(response);
        } catch (TransferException e) {
            logger.warn("Transfer not found: {}", transferId);
            throw e;
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Money Transfer Service");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Root endpoint for basic connectivity test.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Money Transfer Service is running");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("port", System.getProperty("server.port", "8080"));
        response.put("profile", System.getProperty("spring.profiles.active", "default"));
        return ResponseEntity.ok(response);
    }

    /**
     * Simple ping endpoint for health checks.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
} 