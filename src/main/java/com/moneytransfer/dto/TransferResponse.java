package com.moneytransfer.dto;

import com.moneytransfer.domain.Currency;
import com.moneytransfer.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transfer response with comprehensive transfer details.
 */
public class TransferResponse {

    private String transferId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private Currency currency;
    private TransferStatus status;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;

    // Constructors
    public TransferResponse() {}

    public TransferResponse(String transferId, String fromAccountNumber, String toAccountNumber,
                          BigDecimal amount, Currency currency, TransferStatus status,
                          String description, String failureReason, LocalDateTime createdAt,
                          LocalDateTime processedAt, LocalDateTime completedAt) {
        this.transferId = transferId;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "TransferResponse{" +
                "transferId='" + transferId + '\'' +
                ", fromAccountNumber='" + fromAccountNumber + '\'' +
                ", toAccountNumber='" + toAccountNumber + '\'' +
                ", amount=" + amount +
                ", currency=" + currency +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", createdAt=" + createdAt +
                ", processedAt=" + processedAt +
                ", completedAt=" + completedAt +
                '}';
    }
} 