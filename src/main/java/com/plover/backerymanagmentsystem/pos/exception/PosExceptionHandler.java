package com.plover.backerymanagmentsystem.pos.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for POS module
 */
@RestControllerAdvice(basePackages = "com.plover.backerymanagmentsystem.pos")
@Slf4j
public class PosExceptionHandler {

    /**
     * Handle user configuration exceptions
     */
    @ExceptionHandler(UserConfigurationException.class)
    public ResponseEntity<Map<String, Object>> handleUserConfigurationException(UserConfigurationException e) {
        log.warn("User configuration error: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "USER_CONFIGURATION_ERROR");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle GTN not found exceptions
     */
    @ExceptionHandler(GtnNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGtnNotFoundException(GtnNotFoundException e) {
        log.warn("GTN not found: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "GTN_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle GTN processing exceptions
     */
    @ExceptionHandler(GtnProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleGtnProcessingException(GtnProcessingException e) {
        log.error("GTN processing error: {}", e.getMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "GTN_PROCESSING_ERROR");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle general GTN exceptions
     */
    @ExceptionHandler(GtnException.class)
    public ResponseEntity<Map<String, Object>> handleGtnException(GtnException e) {
        log.error("GTN error: {}", e.getMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "GTN_ERROR");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle sale exceptions
     */
    @ExceptionHandler(SaleException.class)
    public ResponseEntity<Map<String, Object>> handleSaleException(SaleException e) {
        log.warn("Sale error: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "SALE_ERROR");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle sale not found exceptions
     */
    @ExceptionHandler(SaleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSaleNotFoundException(SaleNotFoundException e) {
        log.warn("Sale not found: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "SALE_NOT_FOUND");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle invalid payment method exceptions
     */
    @ExceptionHandler(InvalidPaymentMethodException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPaymentMethodException(InvalidPaymentMethodException e) {
        log.warn("Invalid payment method: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "INVALID_PAYMENT_METHOD");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle day-end processing exceptions
     */
    @ExceptionHandler(DayEndException.class)
    public ResponseEntity<Map<String, Object>> handleDayEndException(DayEndException e) {
        log.warn("Day-end validation error: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "DAY_END_VALIDATION_ERROR");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle invalid cashier exceptions
     */
    @ExceptionHandler(InvalidCashierException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCashierException(InvalidCashierException e) {
        log.warn("Invalid cashier: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "INVALID_CASHIER");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle invalid day production item exceptions
     */
    @ExceptionHandler(InvalidDayProductionItemException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDayProductionItemException(InvalidDayProductionItemException e) {
        log.warn("Invalid day production item: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "INVALID_DAY_PRODUCTION_ITEM");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle multiple payment methods exceptions
     */
    @ExceptionHandler(MultiplePaymentMethodsException.class)
    public ResponseEntity<Map<String, Object>> handleMultiplePaymentMethodsException(MultiplePaymentMethodsException e) {
        log.warn("Multiple payment methods error: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "MULTIPLE_PAYMENT_METHODS");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.warn("Runtime exception in POS: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument exception in POS: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        response.put("error", "BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        log.error("Database constraint violation in POS: {}", e.getMessage(), e);
        String msg = parseHumanReadableDbError(e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", msg);
        response.put("error", "CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    private String parseHumanReadableDbError(Throwable e) {
        if (e == null) return "A database operation failed. Please check your entries.";
        
        StringBuilder fullMsgBuilder = new StringBuilder(e.getMessage() != null ? e.getMessage() : "");
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
            if (root.getMessage() != null) {
                fullMsgBuilder.append(" ").append(root.getMessage());
            }
        }
        String fullMsg = fullMsgBuilder.toString();

        if (fullMsg.contains("Duplicate entry")) {
            return parseDuplicateEntryMessage(fullMsg);
        } else if (fullMsg.contains("Data truncation") || fullMsg.contains("Data too long")) {
            return "Invalid data length: One of the input fields exceeds the maximum allowed length.";
        } else if (fullMsg.contains("foreign key constraint") || fullMsg.contains("a foreign key constraint fails")) {
            return "Cannot complete operation because the record is linked to other data.";
        } else if (fullMsg.contains("cannot be null") || (fullMsg.contains("Column") && fullMsg.contains("null"))) {
            return "A required field was missing when saving the transaction.";
        }

        return "Database constraint violation: Please verify your transaction data and try again.";
    }

    private String parseDuplicateEntryMessage(String originalMsg) {
        if (originalMsg == null) {
            return "Duplicate entry detected. The specified value is already in use.";
        }
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Duplicate entry '([^']*)' for key '([^']*)'");
            java.util.regex.Matcher matcher = pattern.matcher(originalMsg);
            if (matcher.find()) {
                String value = matcher.group(1);
                String key = matcher.group(2);
                
                if (key.contains(".")) {
                    key = key.substring(key.lastIndexOf(".") + 1);
                }
                
                String fieldName = key;
                if ("waiter_id".equalsIgnoreCase(key) || key.toLowerCase().contains("waiter")) {
                    fieldName = "Waiter ID";
                } else if ("username".equalsIgnoreCase(key) || key.toLowerCase().contains("username")) {
                    fieldName = "Username";
                } else if ("email".equalsIgnoreCase(key) || key.toLowerCase().contains("email")) {
                    fieldName = "Email";
                } else if ("phone".equalsIgnoreCase(key) || key.toLowerCase().contains("phone")) {
                    fieldName = "Contact Number";
                } else if ("promo_code".equalsIgnoreCase(key) || key.toLowerCase().contains("promo")) {
                    fieldName = "Promo Code";
                } else if ("PRIMARY".equalsIgnoreCase(key)) {
                    fieldName = "Primary Key (ID)";
                }
                
                if (value == null || value.trim().isEmpty()) {
                    return "Duplicate entry detected: That " + fieldName + " is already in use.";
                }
                return "Duplicate entry: '" + value + "' is already in use as a " + fieldName + ".";
            }
        } catch (Exception ex) {
            // fallback
        }
        return "Duplicate entry detected. The specified value is already in use.";
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("Unexpected error in POS module: {}", e.getMessage(), e);

        String msg = e.getMessage();
        if (msg != null && (msg.contains("could not execute statement") || msg.contains("Hibernate") || msg.contains("SQL") || msg.contains("JDBC") || msg.contains("Data truncation"))) {
            msg = parseHumanReadableDbError(e);
        } else if (msg == null || msg.trim().isEmpty()) {
            msg = "An unexpected error occurred while processing the request.";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", msg);
        response.put("error", "INTERNAL_SERVER_ERROR");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
