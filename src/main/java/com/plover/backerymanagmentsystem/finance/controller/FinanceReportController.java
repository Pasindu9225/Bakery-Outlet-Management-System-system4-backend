package com.plover.backerymanagmentsystem.finance.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.finance.dto.ReportEnvelopeDto;
import com.plover.backerymanagmentsystem.finance.service.FinancialReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing the FR-FIN-04 financial report endpoints. All
 * endpoints accept {@code period}, {@code from}, {@code to}, {@code type},
 * and {@code outlet} query parameters where applicable; unsupported params
 * are simply ignored by the underlying service.
 */
@RestController
@RequestMapping("/api/v1/finance/reports")
@RequiredArgsConstructor
@Slf4j
public class FinanceReportController {

    private final FinancialReportService reportService;

    @GetMapping("/payments")
    public ResponseEntity<ReportEnvelopeDto> getPayments(
            @RequestParam(required = false, defaultValue = "Monthly") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Reports: GET /payments period={} from={} to={}", period, from, to);
        return ResponseEntity.ok(reportService.getPaymentSummary(
                period, parseDate(from), parseDate(to)));
    }

    @GetMapping("/sales")
    public ResponseEntity<ReportEnvelopeDto> getSales(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long outlet) {
        log.info("Reports: GET /sales type={} from={} to={} outlet={}", type, from, to, outlet);
        return ResponseEntity.ok(reportService.getSalesReport(
                type, parseDate(from), parseDate(to), outlet));
    }

    @GetMapping("/wastage")
    public ResponseEntity<ReportEnvelopeDto> getWastage(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Long outlet) {
        log.info("Reports: GET /wastage type={} from={} to={} outlet={}", type, from, to, outlet);
        return ResponseEntity.ok(reportService.getWastageReport(
                type, parseDate(from), parseDate(to), outlet));
    }

    @GetMapping("/profitability")
    public ResponseEntity<ReportEnvelopeDto> getProfitability(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Reports: GET /profitability type={} from={} to={}", type, from, to);
        return ResponseEntity.ok(reportService.getProfitabilityReport(
                type, parseDate(from), parseDate(to)));
    }

    @GetMapping("/staff-meals")
    public ResponseEntity<ReportEnvelopeDto> getStaffMeals(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Reports: GET /staff-meals from={} to={}", from, to);
        return ResponseEntity.ok(reportService.getStaffMealReport(
                parseDate(from), parseDate(to)));
    }

    @GetMapping("/stock-movement")
    public ResponseEntity<ReportEnvelopeDto> getStockMovement(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Reports: GET /stock-movement type={} from={} to={}", type, from, to);
        return ResponseEntity.ok(reportService.getStockMovementReport(
                type, parseDate(from), parseDate(to)));
    }

    @GetMapping("/purchase-price")
    public ResponseEntity<ReportEnvelopeDto> getPurchasePrice(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Reports: GET /purchase-price type={} from={} to={}", type, from, to);
        return ResponseEntity.ok(reportService.getPurchasePriceReport(
                type, parseDate(from), parseDate(to)));
    }

    @GetMapping("/variance")
    public ResponseEntity<ReportEnvelopeDto> getVariance(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("Reports: GET /variance type={} from={} to={}", type, from, to);
        return ResponseEntity.ok(reportService.getVarianceReport(
                type, parseDate(from), parseDate(to)));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid date '" + value + "'. Expected ISO-8601 (yyyy-MM-dd)", ex);
        }
    }
}
