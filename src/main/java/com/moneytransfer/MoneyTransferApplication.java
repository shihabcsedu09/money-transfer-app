package com.moneytransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for the Money Transfer Application.
 * 
 * This application provides a robust money transfer service with:
 * - Concurrency control using distributed locking
 * - Atomic transactions with proper rollback mechanisms
 * - Deadlock prevention strategies
 * - Comprehensive monitoring and observability
 * - Security measures for financial operations
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableAsync
public class MoneyTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferApplication.class, args);
    }
} 