package com.plover.backerymanagmentsystem.mis.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.mis.dto.WastageDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageRecordDto;
import com.plover.backerymanagmentsystem.mis.service.WastageReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing the FR-MIS-01 wastage dashboard endpoints.
 *
 * <p>{@code /dashboard} returns the full envelope (summary + groupings +
 * trend + sample records) so the page can render in a single round-trip.
 * {@code /records} returns the same drill-down list paginated, for the
 * detailed table.</p>
 */
@RestController
@RequestMapping("/api/v1/mis/wastage")
@RequiredArgsConstructor
@Slf4j
public class MisWastageController {

    private final WastageReportService wastageReportService;

    @GetMapping("/dashboard")
    public ResponseEntity<WastageDashboardDto> getDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String reason) {
        log.info("MIS: GET /wastage/dashboard startDate={} endDate={} outletId={} productId={} reason={}",
                startDate, endDate, outletId, productId, reason);
        WastageDashboardDto envelope = wastageReportService.getDashboard(
                parseDate("startDate", startDate),
                parseDate("endDate", endDate),
                outletId, productId, reason);
        return ResponseEntity.ok(envelope);
    }

    @GetMapping("/records")
    public ResponseEntity<List<WastageRecordDto>> getRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        log.info("MIS: GET /wastage/records page={} size={}", page, size);
        return ResponseEntity.ok(wastageReportService.getRecords(
                parseDate("startDate", startDate),
                parseDate("endDate", endDate),
                outletId, productId, reason, page, size));
    }

    private LocalDate parseDate(String name, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid " + name + " '" + value + "'. Expected ISO-8601 (yyyy-MM-dd)");
        }
    }
}
