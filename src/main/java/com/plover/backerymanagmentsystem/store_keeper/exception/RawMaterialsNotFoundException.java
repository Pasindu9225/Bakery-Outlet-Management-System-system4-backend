package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when no raw materials are found in the system that meet the
 * criteria.
 */
public class RawMaterialsNotFoundException extends RuntimeException {

    public RawMaterialsNotFoundException() {
        super("No raw materials found with current stock greater than 0");
    }

    public RawMaterialsNotFoundException(String message) {
        super(message);
    }
}
