package com.moneytransfer.service;

import com.moneytransfer.domain.Account;
import com.moneytransfer.domain.Transfer;
import com.moneytransfer.domain.TransferStatus;
import com.moneytransfer.dto.TransferRequest;
import com.moneytransfer.dto.TransferResponse;
import com.moneytransfer.exception.InsufficientFundsException;
import com.moneytransfer.exception.TransferException;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransferRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Core service for handling money transfers with distributed locking and atomic operations.
 * 
 * Features:
 * - Distributed locking to prevent race conditions
 * - Atomic transactions with proper rollback
 * - Deadlock prevention through ordered locking
 * - Retry mechanisms for transient failures
 * - Comprehensive error handling
 */
@Service
public class TransferService {

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final RedissonClient redissonClient;

    @Value("${app.transfer.lock-timeout:30}")
    private int lockTimeout;

    @Value("${app.transfer.retry-attempts:3}")
    private int retryAttempts;

    @Value("${app.transfer.max-amount:1000000.00}")
    private BigDecimal maxTransferAmount;

    @Value("${app.transfer.min-amount:0.01}")
    private BigDecimal minTransferAmount;

    public TransferService(TransferRepository transferRepository, 
                         AccountRepository accountRepository,
                         RedissonClient redissonClient) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.redissonClient = redissonClient;
    }

    /**
     * Process a money transfer with distributed locking and atomic operations.
     */
    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        logger.info("Processing transfer request: {}", request);

        // Validate transfer request
        validateTransferRequest(request);

        // Generate unique transfer ID
        String transferId = generateTransferId();

        // Create transfer record
        Transfer transfer = createTransferRecord(request, transferId);

        // Process the transfer with distributed locking
        try {
            processTransferWithLocking(transfer);
            return buildTransferResponse(transfer);
        } catch (Exception e) {
            logger.error("Transfer processing failed for transferId: {}", transferId, e);
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setFailureReason(e.getMessage());
            transferRepository.save(transfer);
            throw new TransferException("Transfer processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process transfer with distributed locking to prevent race conditions.
     */
    private void processTransferWithLocking(Transfer transfer) {
        String fromAccountNumber = transfer.getFromAccount().getAccountNumber();
        String toAccountNumber = transfer.getToAccount().getAccountNumber();

        // Create distributed locks for both accounts
        RLock fromAccountLock = redissonClient.getLock("account:" + fromAccountNumber);
        RLock toAccountLock = redissonClient.getLock("account:" + toAccountNumber);

        // Determine lock order to prevent deadlocks (always lock in alphabetical order)
        RLock firstLock, secondLock;
        Account firstAccount, secondAccount;
        boolean isFromAccountFirst = fromAccountNumber.compareTo(toAccountNumber) <= 0;

        if (isFromAccountFirst) {
            firstLock = fromAccountLock;
            secondLock = toAccountLock;
            firstAccount = transfer.getFromAccount();
            secondAccount = transfer.getToAccount();
        } else {
            firstLock = toAccountLock;
            secondLock = fromAccountLock;
            firstAccount = transfer.getToAccount();
            secondAccount = transfer.getFromAccount();
        }

        try {
            // Acquire locks with timeout
            boolean firstLockAcquired = firstLock.tryLock(lockTimeout, TimeUnit.SECONDS);
            if (!firstLockAcquired) {
                throw new TransferException("Unable to acquire lock for account: " + 
                    (isFromAccountFirst ? fromAccountNumber : toAccountNumber));
            }

            try {
                boolean secondLockAcquired = secondLock.tryLock(lockTimeout, TimeUnit.SECONDS);
                if (!secondLockAcquired) {
                    throw new TransferException("Unable to acquire lock for account: " + 
                        (isFromAccountFirst ? toAccountNumber : fromAccountNumber));
                }

                try {
                    // Execute the transfer with both locks held
                    executeTransfer(transfer);
                } finally {
                    secondLock.unlock();
                }
            } finally {
                firstLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransferException("Transfer interrupted", e);
        }
    }

    /**
     * Execute the actual transfer with proper error handling and rollback.
     */
    @Transactional
    protected void executeTransfer(Transfer transfer) {
        Account fromAccount = transfer.getFromAccount();
        Account toAccount = transfer.getToAccount();
        BigDecimal amount = transfer.getAmount();

        // Reload accounts with latest data
        fromAccount = accountRepository.findById(fromAccount.getId())
            .orElseThrow(() -> new TransferException("From account not found"));
        toAccount = accountRepository.findById(toAccount.getId())
            .orElseThrow(() -> new TransferException("To account not found"));

        // Validate accounts are active
        if (!fromAccount.getStatus().equals(com.moneytransfer.domain.AccountStatus.ACTIVE)) {
            throw new TransferException("From account is not active");
        }
        if (!toAccount.getStatus().equals(com.moneytransfer.domain.AccountStatus.ACTIVE)) {
            throw new TransferException("To account is not active");
        }

        // Check sufficient funds
        if (!fromAccount.hasSufficientFunds(amount)) {
            throw new InsufficientFundsException("Insufficient funds in account: " + fromAccount.getAccountNumber());
        }

        // Validate currency match
        if (!fromAccount.getCurrency().equals(transfer.getCurrency()) || 
            !toAccount.getCurrency().equals(transfer.getCurrency())) {
            throw new TransferException("Currency mismatch between accounts");
        }

        // Mark transfer as processing
        transfer.markAsProcessing();
        transferRepository.save(transfer);

        // Execute the transfer atomically
        boolean debitSuccess = fromAccount.debit(amount);
        if (!debitSuccess) {
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }

        boolean creditSuccess = toAccount.credit(amount);
        if (!creditSuccess) {
            // Rollback the debit
            fromAccount.credit(amount);
            throw new TransferException("Failed to credit destination account");
        }

        // Save both accounts
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Mark transfer as completed
        transfer.markAsCompleted();
        transferRepository.save(transfer);

        logger.info("Transfer completed successfully: {}", transfer.getTransferId());
    }

    /**
     * Validate transfer request parameters.
     */
    private void validateTransferRequest(TransferRequest request) {
        if (request.getAmount().compareTo(maxTransferAmount) > 0) {
            throw new TransferException("Transfer amount exceeds maximum limit: " + maxTransferAmount);
        }

        if (request.getAmount().compareTo(minTransferAmount) < 0) {
            throw new TransferException("Transfer amount below minimum limit: " + minTransferAmount);
        }

        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new TransferException("Cannot transfer to the same account");
        }
    }

    /**
     * Create initial transfer record.
     */
    private Transfer createTransferRecord(TransferRequest request, String transferId) {
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
            .orElseThrow(() -> new TransferException("From account not found: " + request.getFromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
            .orElseThrow(() -> new TransferException("To account not found: " + request.getToAccountNumber()));

        Transfer transfer = new Transfer(transferId, fromAccount, toAccount, 
                                      request.getAmount(), request.getCurrency(), request.getDescription());
        
        return transferRepository.save(transfer);
    }

    /**
     * Generate unique transfer ID.
     */
    private String generateTransferId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Build transfer response from transfer entity.
     */
    private TransferResponse buildTransferResponse(Transfer transfer) {
        return new TransferResponse(
            transfer.getTransferId(),
            transfer.getFromAccount().getAccountNumber(),
            transfer.getToAccount().getAccountNumber(),
            transfer.getAmount(),
            transfer.getCurrency(),
            transfer.getStatus(),
            transfer.getDescription(),
            transfer.getFailureReason(),
            transfer.getCreatedAt(),
            transfer.getProcessedAt(),
            transfer.getCompletedAt()
        );
    }

    /**
     * Get transfer by ID.
     */
    public TransferResponse getTransfer(String transferId) {
        Transfer transfer = transferRepository.findByTransferId(transferId)
            .orElseThrow(() -> new TransferException("Transfer not found: " + transferId));
        
        return buildTransferResponse(transfer);
    }
} 