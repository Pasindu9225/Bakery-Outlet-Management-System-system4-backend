package com.plover.backerymanagmentsystem.manager.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentException;
import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentNotFoundException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse error = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message("Validation failed")
                .fieldErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(StockAdjustmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockAdjustmentNotFound(StockAdjustmentNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Stock Adjustment Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(StockAdjustmentException.class)
    public ResponseEntity<ErrorResponse> handleStockAdjustmentException(StockAdjustmentException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Stock Adjustment Error")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = "A database integrity violation occurred. This is likely due to a duplicate entry (such as a duplicate code or name).";
        
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("Duplicate entry")) {
                msg = parseDuplicateEntryMessage(cause.getMessage());
                break;
            }
            cause = cause.getCause();
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(msg)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ex.printStackTrace(); // Log the stack trace to console
        
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("Duplicate entry")) {
                ErrorResponse error = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error("Conflict")
                        .message(parseDuplicateEntryMessage(cause.getMessage()))
                        .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }
            cause = cause.getCause();
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
        } catch (Exception e) {
            // fallback
        }
        return "Duplicate entry detected. The specified value is already in use.";
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {

        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationErrorResponse extends ErrorResponse {

        private Map<String, String> fieldErrors;
    }
}
