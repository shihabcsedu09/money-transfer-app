package com.moneytransfer.domain;

/**
 * Enum representing supported currencies in the money transfer application.
 */
public enum Currency {
    USD("US Dollar"),
    EUR("Euro"),
    GBP("British Pound"),
    JPY("Japanese Yen"),
    CAD("Canadian Dollar"),
    AUD("Australian Dollar"),
    CHF("Swiss Franc"),
    CNY("Chinese Yuan"),
    INR("Indian Rupee"),
    BRL("Brazilian Real");

    private final String displayName;

    Currency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 