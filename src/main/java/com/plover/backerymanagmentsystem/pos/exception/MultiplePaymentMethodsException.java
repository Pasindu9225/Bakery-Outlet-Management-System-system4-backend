package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when different payment methods are used in the same sale
 */
public class MultiplePaymentMethodsException extends SaleException {

    public MultiplePaymentMethodsException() {
        super("All items in a sale must use the same payment method");
    }

    public MultiplePaymentMethodsException(String message) {
        super(message);
    }
}
