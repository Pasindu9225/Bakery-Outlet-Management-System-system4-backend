package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when day production item is invalid or not found
 */
public class InvalidDayProductionItemException extends SaleException {

    public InvalidDayProductionItemException(Integer dayProductionItemId) {
        super("Day production item with ID " + dayProductionItemId + " not found");
    }

    public InvalidDayProductionItemException(String message) {
        super(message);
    }
}
