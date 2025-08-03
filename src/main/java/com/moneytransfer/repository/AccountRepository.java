package com.moneytransfer.repository;

import com.moneytransfer.domain.Account;
import com.moneytransfer.domain.AccountStatus;
import com.moneytransfer.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity with custom query methods.
 * 
 * Includes:
 * - Pessimistic locking for critical operations
 * - Custom queries for business logic
 * - Optimized queries with proper indexing
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find account by account number with pessimistic lock for transfer operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account by account number without locking for read operations.
     */
    Optional<Account> findByAccountNumberAndStatus(String accountNumber, AccountStatus status);

    /**
     * Find all accounts for a specific user.
     */
    List<Account> findByUserIdAndStatus(String userId, AccountStatus status);

    /**
     * Find accounts with balance greater than specified amount.
     */
    @Query("SELECT a FROM Account a WHERE a.balance >= :minBalance AND a.status = :status")
    List<Account> findAccountsWithMinimumBalance(@Param("minBalance") BigDecimal minBalance, 
                                               @Param("status") AccountStatus status);

    /**
     * Find accounts by currency and status.
     */
    List<Account> findByCurrencyAndStatus(Currency currency, AccountStatus status);

    /**
     * Count accounts by user ID.
     */
    long countByUserId(String userId);

    /**
     * Find accounts with balance in range.
     */
    @Query("SELECT a FROM Account a WHERE a.balance BETWEEN :minBalance AND :maxBalance AND a.status = :status")
    List<Account> findAccountsByBalanceRange(@Param("minBalance") BigDecimal minBalance,
                                           @Param("maxBalance") BigDecimal maxBalance,
                                           @Param("status") AccountStatus status);

    /**
     * Check if account number exists.
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find account by ID with pessimistic lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findById(Long id);
} 