package com.moneytransfer.controller;

import com.moneytransfer.dto.TransferRequest;
import com.moneytransfer.dto.TransferResponse;
import com.moneytransfer.exception.InsufficientFundsException;
import com.moneytransfer.exception.TransferException;
import com.moneytransfer.service.TransferService;
import com.moneytransfer.repository.AccountRepository;
import java.util.stream.Collectors;
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
    private final AccountRepository accountRepository;

    public TransferController(TransferService transferService, AccountRepository accountRepository) {
        this.transferService = transferService;
        this.accountRepository = accountRepository;
    }

    /**
     * Process a money transfer.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> processTransfer(@Valid @RequestBody TransferRequest request) {
        logger.info("Received transfer request: fromAccount={}, toAccount={}, amount={}, currency={}", 
                   request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount(), request.getCurrency());
        
        try {
            TransferResponse response = transferService.processTransfer(request);
            logger.info("Transfer processed successfully: transferId={}, status={}", response.getTransferId(), response.getStatus());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InsufficientFundsException e) {
            logger.warn("Insufficient funds for transfer: {}", e.getMessage());
            throw e;
        } catch (TransferException e) {
            logger.error("Transfer processing failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during transfer: {}", e.getMessage(), e);
            throw new TransferException("Unexpected error occurred: " + e.getMessage());
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
     * Check if accounts exist (for debugging).
     */
    @GetMapping("/debug/accounts")
    public ResponseEntity<Map<String, Object>> checkAccounts() {
        logger.info("Checking accounts in database...");
        
        Map<String, Object> response = new HashMap<>();
        long accountCount = accountRepository.count();
        response.put("totalAccounts", accountCount);
        
        if (accountCount > 0) {
            response.put("sampleAccounts", accountRepository.findAll().stream()
                .limit(5)
                .map(account -> Map.of(
                    "accountNumber", account.getAccountNumber(),
                    "balance", account.getBalance(),
                    "currency", account.getCurrency().name()
                ))
                .collect(Collectors.toList()));
        }
        
        response.put("message", accountCount > 0 ? "Accounts found" : "No accounts found");
        return ResponseEntity.ok(response);
    }


} 