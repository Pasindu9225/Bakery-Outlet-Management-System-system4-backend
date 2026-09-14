package com.plover.backerymanagmentsystem.mis.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.mis.dto.PurchaseRecordDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchasingDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.TrendPointDto;
import com.plover.backerymanagmentsystem.mis.service.PurchasingTrendsService;
import com.plover.backerymanagmentsystem.mis.service.PurchasingTrendsService.Granularity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing the FR-MIS-03 purchasing-trends endpoints.
 *
 * <ul>
 *   <li>{@code GET /dashboard} — full envelope (summary + trend +
 *       categoryBreakdown + supplierRanking + records).</li>
 *   <li>{@code GET /trend} — just the dual-line series; useful when the
 *       user toggles granularity without needing the rest.</li>
 *   <li>{@code GET /records} — paginated drill-down rows.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/mis/purchasing")
@RequiredArgsConstructor
@Slf4j
public class MisPurchasingController {

    private static final int MAX_PAGE_SIZE = 200;

    private final PurchasingTrendsService purchasingTrendsService;

    @GetMapping("/dashboard")
    public ResponseEntity<PurchasingDashboardDto> getDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) String granularity) {
        log.info("MIS: GET /purchasing/dashboard startDate={} endDate={} supplierId={} category={} outletId={} granularity={}",
                startDate, endDate, supplierId, category, outletId, granularity);
        return ResponseEntity.ok(purchasingTrendsService.getDashboard(
                parseDate("startDate", startDate),
                parseDate("endDate", endDate),
                supplierId, category, outletId, parseGranularity(granularity)));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendPointDto>> getTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) String granularity) {
        log.info("MIS: GET /purchasing/trend granularity={}", granularity);
        return ResponseEntity.ok(purchasingTrendsService.getTrend(
                parseDate("startDate", startDate),
                parseDate("endDate", endDate),
                supplierId, category, outletId, parseGranularity(granularity)));
    }

    @GetMapping("/records")
    public ResponseEntity<Page<PurchaseRecordDto>> getRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        log.info("MIS: GET /purchasing/records page={} size={}", page, size);
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(purchasingTrendsService.getRecords(
                parseDate("startDate", startDate),
                parseDate("endDate", endDate),
                supplierId, category, outletId, search, pageable));
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

    private Granularity parseGranularity(String raw) {
        try {
            return Granularity.from(raw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
