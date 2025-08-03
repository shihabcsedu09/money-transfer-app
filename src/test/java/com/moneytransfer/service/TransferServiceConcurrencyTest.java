package com.moneytransfer.service;

import com.moneytransfer.domain.Account;
import com.moneytransfer.domain.Currency;
import com.moneytransfer.dto.TransferRequest;
import com.moneytransfer.dto.TransferResponse;
import com.moneytransfer.exception.InsufficientFundsException;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive concurrency test for the transfer service.
 * 
 * This test demonstrates:
 * - Thread safety of transfer operations
 * - Atomicity of transactions
 * - Deadlock prevention
 * - Proper handling of concurrent transfers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cache.type=simple",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
public class TransferServiceConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Create test accounts
        account1 = new Account("TEST001234567890", "user1", "Test User 1", Currency.USD);
        account1.setBalance(new BigDecimal("1000.00"));
        account1 = accountRepository.save(account1);

        account2 = new Account("TEST002345678901", "user2", "Test User 2", Currency.USD);
        account2.setBalance(new BigDecimal("500.00"));
        account2 = accountRepository.save(account2);
    }

    @Test
    void testConcurrentTransfers() throws InterruptedException {
        int numberOfThreads = 10;
        int transfersPerThread = 5;
        BigDecimal transferAmount = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Submit concurrent transfer tasks
        for (int i = 0; i < numberOfThreads; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < transfersPerThread; j++) {
                    try {
                        TransferRequest request = new TransferRequest(
                            account1.getAccountNumber(),
                            account2.getAccountNumber(),
                            transferAmount,
                            Currency.USD,
                            "Concurrent transfer test"
                        );
                        TransferResponse response = transferService.processTransfer(request);
                        assertNotNull(response);
                        assertEquals("COMPLETED", response.getStatus().name());
                    } catch (Exception e) {
                        fail("Transfer failed: " + e.getMessage());
                    }
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all transfers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Verify final balances
        Account finalAccount1 = accountRepository.findByAccountNumber(account1.getAccountNumber()).orElse(null);
        Account finalAccount2 = accountRepository.findByAccountNumber(account2.getAccountNumber()).orElse(null);

        assertNotNull(finalAccount1);
        assertNotNull(finalAccount2);

        BigDecimal expectedBalance1 = new BigDecimal("1000.00")
            .subtract(transferAmount.multiply(new BigDecimal(numberOfThreads * transfersPerThread)));
        BigDecimal expectedBalance2 = new BigDecimal("500.00")
            .add(transferAmount.multiply(new BigDecimal(numberOfThreads * transfersPerThread)));

        assertEquals(0, expectedBalance1.compareTo(finalAccount1.getBalance()), 
            "Account 1 balance mismatch");
        assertEquals(0, expectedBalance2.compareTo(finalAccount2.getBalance()), 
            "Account 2 balance mismatch");
    }

    @Test
    void testBidirectionalConcurrentTransfers() throws InterruptedException {
        int numberOfThreads = 5;
        BigDecimal transferAmount = new BigDecimal("5.00");

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Submit bidirectional transfers
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            
            // Transfer from account1 to account2
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                try {
                    TransferRequest request = new TransferRequest(
                        account1.getAccountNumber(),
                        account2.getAccountNumber(),
                        transferAmount,
                        Currency.USD,
                        "Bidirectional transfer test " + threadId
                    );
                    TransferResponse response = transferService.processTransfer(request);
                    assertNotNull(response);
                    assertEquals("COMPLETED", response.getStatus().name());
                } catch (Exception e) {
                    fail("Transfer 1 failed: " + e.getMessage());
                }
            }, executor);
            futures.add(future1);

            // Transfer from account2 to account1
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                try {
                    TransferRequest request = new TransferRequest(
                        account2.getAccountNumber(),
                        account1.getAccountNumber(),
                        transferAmount,
                        Currency.USD,
                        "Bidirectional transfer test " + threadId
                    );
                    TransferResponse response = transferService.processTransfer(request);
                    assertNotNull(response);
                    assertEquals("COMPLETED", response.getStatus().name());
                } catch (Exception e) {
                    fail("Transfer 2 failed: " + e.getMessage());
                }
            }, executor);
            futures.add(future2);
        }

        // Wait for all transfers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Verify final balances (should be the same as initial)
        Account finalAccount1 = accountRepository.findByAccountNumber(account1.getAccountNumber()).orElse(null);
        Account finalAccount2 = accountRepository.findByAccountNumber(account2.getAccountNumber()).orElse(null);

        assertNotNull(finalAccount1);
        assertNotNull(finalAccount2);

        assertEquals(0, new BigDecimal("1000.00").compareTo(finalAccount1.getBalance()), 
            "Account 1 balance should remain unchanged");
        assertEquals(0, new BigDecimal("500.00").compareTo(finalAccount2.getBalance()), 
            "Account 2 balance should remain unchanged");
    }

    @Test
    void testInsufficientFundsHandling() {
        BigDecimal largeAmount = new BigDecimal("2000.00"); // More than account balance

        TransferRequest request = new TransferRequest(
            account2.getAccountNumber(), // Account with only 500.00
            account1.getAccountNumber(),
            largeAmount,
            Currency.USD,
            "Insufficient funds test"
        );

        assertThrows(InsufficientFundsException.class, () -> {
            transferService.processTransfer(request);
        });

        // Verify account balances remain unchanged
        Account finalAccount1 = accountRepository.findByAccountNumber(account1.getAccountNumber()).orElse(null);
        Account finalAccount2 = accountRepository.findByAccountNumber(account2.getAccountNumber()).orElse(null);

        assertNotNull(finalAccount1);
        assertNotNull(finalAccount2);

        assertEquals(0, new BigDecimal("1000.00").compareTo(finalAccount1.getBalance()));
        assertEquals(0, new BigDecimal("500.00").compareTo(finalAccount2.getBalance()));
    }

    @Test
    void testTransferToSameAccount() {
        TransferRequest request = new TransferRequest(
            account1.getAccountNumber(),
            account1.getAccountNumber(), // Same account
            new BigDecimal("10.00"),
            Currency.USD,
            "Same account transfer test"
        );

        assertThrows(Exception.class, () -> {
            transferService.processTransfer(request);
        });
    }

    @Test
    void testConcurrentInsufficientFunds() throws InterruptedException {
        int numberOfThreads = 10;
        BigDecimal transferAmount = new BigDecimal("100.00"); // Each transfer is 100, account has 500

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedTransfers = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Submit concurrent transfers that will eventually fail
        for (int i = 0; i < numberOfThreads; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    TransferRequest request = new TransferRequest(
                        account2.getAccountNumber(), // Account with 500.00
                        account1.getAccountNumber(),
                        transferAmount,
                        Currency.USD,
                        "Concurrent insufficient funds test"
                    );
                    TransferResponse response = transferService.processTransfer(request);
                    assertNotNull(response);
                    successfulTransfers.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    failedTransfers.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all transfers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // Verify results
        assertEquals(5, successfulTransfers.get(), "Should have 5 successful transfers (500/100)");
        assertEquals(5, failedTransfers.get(), "Should have 5 failed transfers");

        // Verify final balance
        Account finalAccount2 = accountRepository.findByAccountNumber(account2.getAccountNumber()).orElse(null);
        assertNotNull(finalAccount2);
        assertEquals(0, BigDecimal.ZERO.compareTo(finalAccount2.getBalance()), 
            "Account should be empty after successful transfers");
    }
} 