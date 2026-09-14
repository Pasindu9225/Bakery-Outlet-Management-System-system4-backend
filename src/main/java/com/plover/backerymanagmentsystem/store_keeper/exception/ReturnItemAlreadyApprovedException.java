package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when attempting to approve already approved return items.
 */
public class ReturnItemAlreadyApprovedException extends RuntimeException {

    public ReturnItemAlreadyApprovedException(Long itemId) {
        super(String.format("Return item with ID %d is already approved", itemId));
    }

    public ReturnItemAlreadyApprovedException(String message) {
        super(message);
    }

    public ReturnItemAlreadyApprovedException(String message, Throwable cause) {
        super(message, cause);
    }
}
