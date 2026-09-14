package com.plover.backerymanagmentsystem.admin.service;

import com.plover.backerymanagmentsystem.admin.dto.TrendResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminReportDashboardService {

    TrendResponseDto getTrends(String dateRange, LocalDate customDateFrom, LocalDate customDateTo, String metric, Boolean compareEnabled);

    List<Map<String, Object>> generateReport(String reportType, LocalDate dateFrom, LocalDate dateTo, String outlet, String category, String employee);
}
