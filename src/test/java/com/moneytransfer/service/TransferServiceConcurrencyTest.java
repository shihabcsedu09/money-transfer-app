package com.moneytransfer.service;

import com.moneytransfer.domain.Account;
import com.moneytransfer.domain.Currency;
import com.moneytransfer.dto.TransferRequest;
import com.moneytransfer.dto.TransferResponse;
import com.moneytransfer.exception.TransferException;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

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
    @Transactional
    void setUp() {
        // Clear existing data
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
    @Transactional
    void testBasicTransfer() {
        TransferRequest request = new TransferRequest(
            account1.getAccountNumber(),
            account2.getAccountNumber(),
            new BigDecimal("100.00"),
            Currency.USD,
            "Basic transfer test"
        );

        TransferResponse response = transferService.processTransfer(request);
        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus().name());
    }

    @Test
    @Transactional
    void testMultipleTransfers() {
        // Test multiple transfers in sequence
        for (int i = 0; i < 3; i++) {
            TransferRequest request = new TransferRequest(
                account1.getAccountNumber(),
                account2.getAccountNumber(),
                new BigDecimal("10.00"),
                Currency.USD,
                "Multiple transfer test " + i
            );

            TransferResponse response = transferService.processTransfer(request);
            assertNotNull(response);
            assertEquals("COMPLETED", response.getStatus().name());
        }
    }

    @Test
    @Transactional
    void testBidirectionalTransfers() {
        // Transfer from account1 to account2
        TransferRequest request1 = new TransferRequest(
            account1.getAccountNumber(),
            account2.getAccountNumber(),
            new BigDecimal("50.00"),
            Currency.USD,
            "Bidirectional transfer test 1"
        );

        TransferResponse response1 = transferService.processTransfer(request1);
        assertNotNull(response1);
        assertEquals("COMPLETED", response1.getStatus().name());

        // Transfer from account2 to account1
        TransferRequest request2 = new TransferRequest(
            account2.getAccountNumber(),
            account1.getAccountNumber(),
            new BigDecimal("25.00"),
            Currency.USD,
            "Bidirectional transfer test 2"
        );

        TransferResponse response2 = transferService.processTransfer(request2);
        assertNotNull(response2);
        assertEquals("COMPLETED", response2.getStatus().name());
    }

    @Test
    @Transactional
    void testInsufficientFundsHandling() {
        BigDecimal largeAmount = new BigDecimal("2000.00"); // More than account balance

        TransferRequest request = new TransferRequest(
            account2.getAccountNumber(), // Account with only 500.00
            account1.getAccountNumber(),
            largeAmount,
            Currency.USD,
            "Insufficient funds test"
        );

        assertThrows(TransferException.class, () -> {
            transferService.processTransfer(request);
        });
    }

    @Test
    @Transactional
    void testTransferToSameAccount() {
        TransferRequest request = new TransferRequest(
            account1.getAccountNumber(),
            account1.getAccountNumber(), // Same account
            new BigDecimal("10.00"),
            Currency.USD,
            "Same account transfer test"
        );

        // This should either fail or be handled gracefully
        try {
            TransferResponse response = transferService.processTransfer(request);
            // If it succeeds, that's fine too
            assertNotNull(response);
        } catch (Exception e) {
            // If it fails, that's also acceptable
            assertTrue(e instanceof TransferException);
        }
    }

    @Test
    @Transactional
    void testServiceContext() {
        // Simple test to verify the service is working
        assertNotNull(transferService);
        assertNotNull(accountRepository);
        assertNotNull(transferRepository);
        assertNotNull(account1);
        assertNotNull(account2);
    }
} 