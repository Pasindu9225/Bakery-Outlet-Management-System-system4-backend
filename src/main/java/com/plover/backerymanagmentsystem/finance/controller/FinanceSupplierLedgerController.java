package com.plover.backerymanagmentsystem.finance.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.finance.dto.CreateManualAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.LedgerEntryDto;
import com.plover.backerymanagmentsystem.finance.dto.ManualAdjustmentResponseDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierForLedgerDto;
import com.plover.backerymanagmentsystem.finance.service.FinanceSupplierService;
import com.plover.backerymanagmentsystem.finance.service.ManualAdjustmentService;
import com.plover.backerymanagmentsystem.finance.service.SupplierLedgerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing supplier-ledger endpoints for FR-FIN-01.
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Slf4j
public class FinanceSupplierLedgerController {

    private final SupplierLedgerService ledgerService;
    private final FinanceSupplierService supplierService;
    private final ManualAdjustmentService adjustmentService;

    /**
     * List all suppliers as lightweight projections suitable for the supplier
     * picker on the ledger screen.
     */
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierForLedgerDto>> getSuppliers() {
        log.info("Finance: GET /suppliers");
        return ResponseEntity.ok(supplierService.getAllSuppliersForLedger());
    }

    /**
     * Get the full ledger for a supplier with optional filters.
     */
    @GetMapping("/suppliers/{id}/ledger")
    public ResponseEntity<List<LedgerEntryDto>> getLedger(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        log.info("Finance: GET /suppliers/{}/ledger?startDate={}&endDate={}&type={}&status={}",
                id, startDate, endDate, type, status);
        LocalDate from = parseDate(startDate);
        LocalDate to = parseDate(endDate);
        return ResponseEntity.ok(ledgerService.getLedger(id, from, to, type, status));
    }

    /**
     * Create a manual ledger adjustment. Restricted to the Finance role at the
     * UI / route level.
     */
    @PostMapping("/manual-adjustments")
    public ResponseEntity<ManualAdjustmentResponseDto> createAdjustment(
            @Valid @RequestBody CreateManualAdjustmentRequestDto request) {
        log.info("Finance: POST /manual-adjustments supplierId={} amount={}", request.getSupplierId(), request.getAmount());
        ManualAdjustmentResponseDto response = adjustmentService.createAdjustment(request);
        return ResponseEntity.ok(response);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date '" + value + "'. Expected ISO-8601 (yyyy-MM-dd)", ex);
        }
    }
}
