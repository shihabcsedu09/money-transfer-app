package com.moneytransfer.repository;

import com.moneytransfer.domain.Transfer;
import com.moneytransfer.domain.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transfer entity with custom query methods.
 * 
 * Includes:
 * - Pagination support for large datasets
 * - Custom queries for reporting and analytics
 * - Optimized queries with proper indexing
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * Find transfer by transfer ID.
     */
    Optional<Transfer> findByTransferId(String transferId);

    /**
     * Find transfers by status.
     */
    List<Transfer> findByStatus(TransferStatus status);

    /**
     * Find transfers by from account.
     */
    Page<Transfer> findByFromAccount_AccountNumber(String fromAccountNumber, Pageable pageable);

    /**
     * Find transfers by to account.
     */
    Page<Transfer> findByToAccount_AccountNumber(String toAccountNumber, Pageable pageable);

    /**
     * Find transfers by account (either from or to).
     */
    @Query("SELECT t FROM Transfer t WHERE t.fromAccount.accountNumber = :accountNumber OR t.toAccount.accountNumber = :accountNumber")
    Page<Transfer> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);

    /**
     * Find transfers by status and date range.
     */
    @Query("SELECT t FROM Transfer t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transfer> findByStatusAndDateRange(@Param("status") TransferStatus status,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find transfers with amount greater than specified value.
     */
    @Query("SELECT t FROM Transfer t WHERE t.amount >= :minAmount AND t.status = :status")
    List<Transfer> findByMinimumAmount(@Param("minAmount") BigDecimal minAmount,
                                     @Param("status") TransferStatus status);

    /**
     * Count transfers by status.
     */
    long countByStatus(TransferStatus status);

    /**
     * Find pending transfers created before specified time.
     */
    @Query("SELECT t FROM Transfer t WHERE t.status = 'PENDING' AND t.createdAt < :beforeTime")
    List<Transfer> findPendingTransfersBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Find transfers by user ID (from account user).
     */
    @Query("SELECT t FROM Transfer t WHERE t.fromAccount.userId = :userId OR t.toAccount.userId = :userId")
    Page<Transfer> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Get total transfer amount by status and date range.
     */
    @Query("SELECT SUM(t.amount) FROM Transfer t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalAmountByStatusAndDateRange(@Param("status") TransferStatus status,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Find transfers that need to be processed (pending for too long).
     */
    @Query("SELECT t FROM Transfer t WHERE t.status = 'PENDING' AND t.createdAt < :cutoffTime")
    List<Transfer> findStalePendingTransfers(@Param("cutoffTime") LocalDateTime cutoffTime);
} 