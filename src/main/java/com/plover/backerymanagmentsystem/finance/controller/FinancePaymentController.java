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

import com.plover.backerymanagmentsystem.finance.dto.CreatePaymentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.OutstandingGrnDto;
import com.plover.backerymanagmentsystem.finance.dto.PaymentResponseDto;
import com.plover.backerymanagmentsystem.finance.service.SupplierPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing the FR-FIN-03 settle-payments endpoints.
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Slf4j
public class FinancePaymentController {

    private final SupplierPaymentService paymentService;

    /**
     * List all GRNs with an outstanding balance for the given supplier.
     */
    @GetMapping("/suppliers/{id}/outstanding-grns")
    public ResponseEntity<List<OutstandingGrnDto>> getOutstandingGrns(@PathVariable Long id) {
        log.info("Finance: GET /suppliers/{}/outstanding-grns", id);
        return ResponseEntity.ok(paymentService.getOutstandingGrns(id));
    }

    /**
     * Record a new supplier payment with one or more allocations.
     */
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody CreatePaymentRequestDto request) {
        log.info("Finance: POST /payments supplierId={} allocations={}",
                request.getSupplierId(),
                request.getAllocations() == null ? 0 : request.getAllocations().size());
        PaymentResponseDto response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * List supplier payments with optional supplier/status/date/search filters.
     */
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponseDto>> listPayments(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search) {
        log.info("Finance: GET /payments supplierId={} status={} from={} to={} search={}",
                supplierId, status, startDate, endDate, search);
        LocalDate from = parseDate(startDate);
        LocalDate to = parseDate(endDate);
        return ResponseEntity.ok(paymentService.listPayments(supplierId, status, from, to, search));
    }

    /**
     * Get a single payment by id.
     */
    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable Long id) {
        log.info("Finance: GET /payments/{}", id);
        return ResponseEntity.ok(paymentService.getPayment(id));
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
