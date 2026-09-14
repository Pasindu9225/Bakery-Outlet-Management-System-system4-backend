package com.plover.backerymanagmentsystem.store_keeper.exception;

import java.time.LocalDate;

/**
 * Exception thrown when an invalid delivery date is provided.
 */
public class InvalidDeliveryDateException extends RuntimeException {

    public InvalidDeliveryDateException(LocalDate deliveryDate) {
        super(String.format("Invalid delivery date: %s. Delivery date must be in the future.", deliveryDate));
    }

    public InvalidDeliveryDateException(String message) {
        super(message);
    }
}
