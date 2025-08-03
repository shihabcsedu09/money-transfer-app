package com.moneytransfer.config;

import com.moneytransfer.domain.Account;
import com.moneytransfer.domain.AccountStatus;
import com.moneytransfer.domain.Currency;
import com.moneytransfer.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Production data initializer to populate the database with sample accounts.
 * Only runs in production profile.
 */
@Configuration
@Profile("prod")
public class ProductionDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ProductionDataInitializer.class);

    @Bean
    public CommandLineRunner initializeProductionData(AccountRepository accountRepository) {
        return args -> {
            logger.info("Initializing production sample data...");

            // Check if data already exists
            if (accountRepository.count() > 0) {
                logger.info("Production database already contains data, skipping initialization.");
                return;
            }

            // Create sample accounts for production
            List<Account> accounts = Arrays.asList(
                createAccount("ACC001234567890", "user1", "John Doe", Currency.USD, new BigDecimal("10000.00")),
                createAccount("ACC002345678901", "user1", "John Doe", Currency.EUR, new BigDecimal("8500.00")),
                createAccount("ACC003456789012", "user2", "Jane Smith", Currency.USD, new BigDecimal("5000.00")),
                createAccount("ACC004567890123", "user2", "Jane Smith", Currency.GBP, new BigDecimal("3000.00")),
                createAccount("ACC005678901234", "user3", "Bob Johnson", Currency.USD, new BigDecimal("7500.00")),
                createAccount("ACC006789012345", "user4", "Alice Brown", Currency.EUR, new BigDecimal("12000.00")),
                createAccount("ACC007890123456", "user5", "Charlie Wilson", Currency.GBP, new BigDecimal("4500.00")),
                createAccount("ACC008901234567", "user6", "Diana Davis", Currency.USD, new BigDecimal("2000.00"))
            );

            // Save all accounts
            accountRepository.saveAll(accounts);

            logger.info("Production sample data initialized successfully. Created {} accounts.", accounts.size());
            
            // Log account details
            accounts.forEach(account -> 
                logger.info("Created production account: {} - {} {} - Balance: {} {}", 
                    account.getAccountNumber(),
                    account.getAccountHolderName(),
                    account.getUserId(),
                    account.getBalance(),
                    account.getCurrency())
            );
        };
    }

    private Account createAccount(String accountNumber, String userId, String holderName, 
                                Currency currency, BigDecimal balance) {
        Account account = new Account(accountNumber, userId, holderName, currency);
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }
} 