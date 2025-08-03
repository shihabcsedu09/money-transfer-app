package com.moneytransfer.exception;

/**
 * Main exception class for transfer-related errors.
 */
public class TransferException extends RuntimeException {

    public TransferException(String message) {
        super(message);
    }

    public TransferException(String message, Throwable cause) {
        super(message, cause);
    }
} 