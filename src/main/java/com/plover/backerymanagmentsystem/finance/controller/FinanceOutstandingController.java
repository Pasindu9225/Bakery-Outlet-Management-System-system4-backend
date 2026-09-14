package com.plover.backerymanagmentsystem.finance.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDetailDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDto;
import com.plover.backerymanagmentsystem.finance.service.OutstandingSummaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing the FR-FIN-02 outstanding-summary endpoints.
 */
@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Slf4j
public class FinanceOutstandingController {

    private final OutstandingSummaryService outstandingSummaryService;

    /**
     * Aggregated, per-supplier outstanding rollup. Suppliers with no unpaid
     * GRNs are excluded by default; pass {@code includeAll=true} to include
     * them.
     */
    @GetMapping("/outstanding-summary")
    public ResponseEntity<List<SupplierOutstandingDto>> getOutstandingSummary(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean overdueOnly,
            @RequestParam(required = false) String dueFrom,
            @RequestParam(required = false) String dueTo,
            @RequestParam(required = false, defaultValue = "false") Boolean includeAll) {
        log.info("Finance: GET /outstanding-summary search={} overdueOnly={} dueFrom={} dueTo={} includeAll={}",
                search, overdueOnly, dueFrom, dueTo, includeAll);
        LocalDate from = parseDate(dueFrom);
        LocalDate to = parseDate(dueTo);
        return ResponseEntity.ok(
                outstandingSummaryService.getSummary(search, overdueOnly, from, to, includeAll));
    }

    /**
     * Drill-down for a single supplier: per-supplier rollup plus the full list
     * of unpaid GRNs.
     */
    @GetMapping("/suppliers/{id}/outstanding-detail")
    public ResponseEntity<SupplierOutstandingDetailDto> getOutstandingDetail(@PathVariable Long id) {
        log.info("Finance: GET /suppliers/{}/outstanding-detail", id);
        return ResponseEntity.ok(outstandingSummaryService.getDetail(id));
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
