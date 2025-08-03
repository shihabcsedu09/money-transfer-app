package com.moneytransfer.exception;

/**
 * Exception thrown when an account has insufficient funds for a transfer.
 */
public class InsufficientFundsException extends TransferException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
} 