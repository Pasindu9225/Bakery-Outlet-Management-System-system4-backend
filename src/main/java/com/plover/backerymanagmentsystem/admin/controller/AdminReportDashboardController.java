package com.plover.backerymanagmentsystem.admin.controller;

import com.plover.backerymanagmentsystem.admin.dto.TrendResponseDto;
import com.plover.backerymanagmentsystem.admin.service.AdminReportDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminReportDashboardController {

    private final AdminReportDashboardService adminReportDashboardService;

    @GetMapping("/trends")
    public ResponseEntity<TrendResponseDto> getTrends(
            @RequestParam(required = false, defaultValue = "Last 7 Days") String dateRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customDateTo,
            @RequestParam(required = false, defaultValue = "Sales") String metric,
            @RequestParam(required = false, defaultValue = "false") Boolean compareEnabled) {
        
        return ResponseEntity.ok(adminReportDashboardService.getTrends(dateRange, customDateFrom, customDateTo, metric, compareEnabled));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> generateReport(
            @RequestParam String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String outlet,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String employee) {
        
        return ResponseEntity.ok(adminReportDashboardService.generateReport(reportType, dateFrom, dateTo, outlet, category, employee));
    }
}
