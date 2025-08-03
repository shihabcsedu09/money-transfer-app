package com.moneytransfer.domain;

/**
 * Enum representing the status of a money transfer.
 */
public enum TransferStatus {
    PENDING("Transfer is pending processing"),
    PROCESSING("Transfer is being processed"),
    COMPLETED("Transfer completed successfully"),
    FAILED("Transfer failed"),
    CANCELLED("Transfer was cancelled"),
    REVERSED("Transfer was reversed");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 