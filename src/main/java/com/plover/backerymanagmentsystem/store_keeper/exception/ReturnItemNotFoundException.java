package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when return items are not found.
 */
public class ReturnItemNotFoundException extends RuntimeException {

    public ReturnItemNotFoundException(Long itemId) {
        super(String.format("Return item not found with ID: %d", itemId));
    }

    public ReturnItemNotFoundException(String message) {
        super(message);
    }

    public ReturnItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
