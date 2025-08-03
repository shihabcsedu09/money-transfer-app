package com.moneytransfer.domain;

/**
 * Enum representing the status of an account.
 */
public enum AccountStatus {
    ACTIVE("Active account"),
    SUSPENDED("Account suspended"),
    CLOSED("Account closed"),
    PENDING("Account pending activation");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 