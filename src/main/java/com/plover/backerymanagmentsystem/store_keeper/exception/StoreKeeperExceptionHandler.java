package com.plover.backerymanagmentsystem.store_keeper.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice(basePackages = "com.plover.backerymanagmentsystem.store_keeper")
@Slf4j
public class StoreKeeperExceptionHandler {

    @ExceptionHandler(ProductionPlanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductionPlanNotFound(ProductionPlanNotFoundException ex) {
        log.error("Production plan not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Production Plan Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/approved-plans/issue-plan")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ProductionPlanNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleProductionPlanNotApproved(ProductionPlanNotApprovedException ex) {
        log.error("Production plan not approved: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Production Plan Not Approved")
                .message(ex.getMessage())
                .path("/STK/v1/approved-plans/issue-plan")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        log.error("Insufficient stock: {}", ex.getMessage());

        Map<String, Object> details = new java.util.HashMap<>();
        details.put("insufficientMaterials", ex.getInsufficientMaterials());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Insufficient Stock")
                .message(ex.getMessage())
                .path("/STK/v1/approved-plans/issue-plan")
                .details(details)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MaterialsAlreadyIssuedException.class)
    public ResponseEntity<ErrorResponse> handleMaterialsAlreadyIssued(MaterialsAlreadyIssuedException ex) {
        log.error("Materials already issued: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Materials Already Issued")
                .message(ex.getMessage())
                .path("/STK/v1/approved-plans/issue-plan")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(RawMaterialNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRawMaterialNotFound(RawMaterialNotFoundException ex) {
        log.error("Raw material not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Raw Material Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/suppliers/by-material")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(NoSuppliersFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoSuppliersFound(NoSuppliersFoundException ex) {
        log.error("No suppliers found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("No Suppliers Found")
                .message(ex.getMessage())
                .path("/STK/v1/suppliers/by-material")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSupplierNotFound(SupplierNotFoundException ex) {
        log.error("Supplier not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Supplier Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/purchase-orders")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(SuppliersNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSuppliersNotFound(SuppliersNotFoundException ex) {
        log.error("Suppliers not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Suppliers Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/suppliers")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(SupplierRawMaterialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSupplierRawMaterialsNotFound(SupplierRawMaterialsNotFoundException ex) {
        log.error("Supplier raw materials not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Supplier Raw Materials Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/suppliers/raw-materials")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseOrderNotFound(PurchaseOrderNotFoundException ex) {
        log.error("Purchase order not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Purchase Order Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/purchase-orders")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(RawMaterialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRawMaterialsNotFound(RawMaterialsNotFoundException ex) {
        log.error("Raw materials not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Raw Materials Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/materials")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(SupplierMaterialMismatchException.class)
    public ResponseEntity<ErrorResponse> handleSupplierMaterialMismatch(SupplierMaterialMismatchException ex) {
        log.error("Supplier-material mismatch: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Supplier Material Mismatch")
                .message(ex.getMessage())
                .path("/STK/v1/purchase-orders")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidDeliveryDateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDeliveryDate(InvalidDeliveryDateException ex) {
        log.error("Invalid delivery date: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Delivery Date")
                .message(ex.getMessage())
                .path("/STK/v1/purchase-orders")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(RawMaterialReturnNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRawMaterialReturnNotFound(RawMaterialReturnNotFoundException ex) {
        log.error("Raw material return not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Raw Material Return Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/returns")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ReturnItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReturnItemNotFound(ReturnItemNotFoundException ex) {
        log.error("Return item not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Return Item Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/returns/approve")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(SupplierMaterialValidationException.class)
    public ResponseEntity<ErrorResponse> handleSupplierMaterialValidation(SupplierMaterialValidationException ex) {
        log.error("Supplier material validation failed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Supplier Material Validation Failed")
                .message(ex.getMessage())
                .path("/STK/v1/returns/create")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InsufficientStockForReturnException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStockForReturn(InsufficientStockForReturnException ex) {
        log.error("Insufficient stock for return: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Insufficient Stock For Return")
                .message(ex.getMessage())
                .path("/STK/v1/returns/create")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ReturnItemAlreadyApprovedException.class)
    public ResponseEntity<ErrorResponse> handleReturnItemAlreadyApproved(ReturnItemAlreadyApprovedException ex) {
        log.error("Return item already approved: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Return Item Already Approved")
                .message(ex.getMessage())
                .path("/STK/v1/returns/approve")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        BindingResult bindingResult = ex.getBindingResult();
        Map<String, String> validationErrors = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        Map<String, Object> details = new java.util.HashMap<>();
        details.put("validationErrors", validationErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters")
                .path("/STK/v1/approved-plans/issue-plan")
                .details(details)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(StockAdjustmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockAdjustmentNotFound(StockAdjustmentNotFoundException ex) {
        log.error("Stock adjustment not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Stock Adjustment Not Found")
                .message(ex.getMessage())
                .path("/STK/v1/stock-adjustments")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(StockAdjustmentException.class)
    public ResponseEntity<ErrorResponse> handleStockAdjustmentException(StockAdjustmentException ex) {
        log.error("Stock adjustment error: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Stock Adjustment Error")
                .message(ex.getMessage())
                .path("/STK/v1/stock-adjustments")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception in StoreKeeper: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path("/STK/v1")
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument exception in StoreKeeper: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path("/STK/v1")
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Database constraint violation in StoreKeeper: {}", ex.getMessage());
        String msg = "A database integrity violation occurred. This is likely due to a duplicate entry (such as a duplicate code or name).";
        
        Throwable cause = ex;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("Duplicate entry")) {
                msg = parseDuplicateEntryMessage(cause.getMessage());
                break;
            }
            cause = cause.getCause();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(msg)
                .path("/STK/v1")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                .path("/STK/v1/materials/all")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
