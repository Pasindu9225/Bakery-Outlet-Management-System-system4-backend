package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when payment method is invalid or not found
 */
public class InvalidPaymentMethodException extends SaleException {

    public InvalidPaymentMethodException(Integer paymentMethodId) {
        super("Payment method with ID " + paymentMethodId + " not found or inactive");
    }

    public InvalidPaymentMethodException(String message) {
        super(message);
    }
}
