package com.plover.backerymanagmentsystem.admin.service.impl;

import com.plover.backerymanagmentsystem.admin.dto.TrendResponseDto;
import com.plover.backerymanagmentsystem.admin.service.AdminReportDashboardService;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.model.SaleItem;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;
import com.plover.backerymanagmentsystem.pos.model.Return;
import com.plover.backerymanagmentsystem.pos.model.ReturnItem;
import com.plover.backerymanagmentsystem.pos.repository.ReturnRepository;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import com.plover.backerymanagmentsystem.admin.repository.BmsAuthRepository;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportDashboardServiceImpl implements AdminReportDashboardService {

    private final SaleRepository saleRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final StockAdjustRepository stockAdjustRepository;
    private final ReturnRepository returnRepository;
    private final OutletRepository outletRepository;
    private final BmsAuthRepository bmsAuthRepository;

    @Override
    public TrendResponseDto getTrends(String dateRange, LocalDate customDateFrom, LocalDate customDateTo, String metric, Boolean compareEnabled) {
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        if ("Last 7 Days".equalsIgnoreCase(dateRange)) {
            startDate = endDate.minusDays(7);
        } else if ("Last 30 Days".equalsIgnoreCase(dateRange)) {
            startDate = endDate.minusDays(30);
        } else if ("This Month".equalsIgnoreCase(dateRange)) {
            startDate = endDate.withDayOfMonth(1);
        } else if ("Last Month".equalsIgnoreCase(dateRange)) {
            startDate = endDate.minusMonths(1).withDayOfMonth(1);
            endDate = startDate.plusMonths(1).minusDays(1);
        } else if ("Custom Range".equalsIgnoreCase(dateRange) && customDateFrom != null && customDateTo != null) {
            startDate = customDateFrom;
            endDate = customDateTo;
        } else {
            startDate = endDate.minusDays(7);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999999999);

        Map<String, Double> currentAggregatedData = new TreeMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            currentAggregatedData.put(date.format(DateTimeFormatter.ISO_LOCAL_DATE), 0.0);
        }

        double totalValue = 0.0;
        double peakValue = 0.0;
        String peakDay = "";

        if ("Production".equalsIgnoreCase(metric)) {
            List<ProductionPlan> plans = productionPlanRepository.findByPlanDateBetween(startDateTime, endDateTime);
            for (ProductionPlan plan : plans) {
                if (plan.getPlanDate() == null) continue;
                String dateKey = plan.getPlanDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (!currentAggregatedData.containsKey(dateKey)) continue;

                double planQty = 0.0;
                if (plan.getProductionPlanItems() != null) {
                    for (ProductionPlanItem item : plan.getProductionPlanItems()) {
                        if (item.getQuantity() != null) {
                            planQty += item.getQuantity();
                        }
                    }
                }
                double newTotal = currentAggregatedData.get(dateKey) + planQty;
                currentAggregatedData.put(dateKey, newTotal);
                totalValue += planQty;
            }
        } else if ("Inventory Movement".equalsIgnoreCase(metric)) {
            List<StockAdjust> adjustments = stockAdjustRepository.findByCreatedAtBetween(startDateTime, endDateTime);
            for (StockAdjust adjust : adjustments) {
                if (adjust.getCreatedAt() == null) continue;
                String dateKey = adjust.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (!currentAggregatedData.containsKey(dateKey)) continue;

                double qty = adjust.getChangeQuantity() != null ? Math.abs(adjust.getChangeQuantity()) : 0.0;
                double newTotal = currentAggregatedData.get(dateKey) + qty;
                currentAggregatedData.put(dateKey, newTotal);
                totalValue += qty;
            }
        } else if ("Returns".equalsIgnoreCase(metric)) {
            List<Return> returns = returnRepository.findByCreatedAtBetween(startDateTime, endDateTime);
            for (Return ret : returns) {
                if (ret.getCreatedAt() == null) continue;
                String dateKey = ret.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (!currentAggregatedData.containsKey(dateKey)) continue;

                double retQty = 0.0;
                if (ret.getReturnItems() != null) {
                    for (ReturnItem item : ret.getReturnItems()) {
                        if (item.getQty() != null) {
                            retQty += item.getQty();
                        }
                    }
                }
                double newTotal = currentAggregatedData.get(dateKey) + retQty;
                currentAggregatedData.put(dateKey, newTotal);
                totalValue += retQty;
            }
        } else if ("POS Transactions".equalsIgnoreCase(metric)) {
            List<Sale> currentSales = saleRepository.findBySaleDateBetween(startDate, endDate);
            for (Sale sale : currentSales) {
                String dateKey = sale.getSaleDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                double newTotal = currentAggregatedData.getOrDefault(dateKey, 0.0) + 1.0;
                currentAggregatedData.put(dateKey, newTotal);
                totalValue += 1.0;
            }
        } else {
            // Default to Sales (Revenue)
            List<Sale> currentSales = saleRepository.findBySaleDateBetween(startDate, endDate);
            for (Sale sale : currentSales) {
                String dateKey = sale.getSaleDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                double amount = sale.getFinalTotal() != null ? sale.getFinalTotal().doubleValue() : sale.getTotalAmount().doubleValue();
                double newTotal = currentAggregatedData.getOrDefault(dateKey, 0.0) + amount;
                currentAggregatedData.put(dateKey, newTotal);
                totalValue += amount;
            }
        }

        // Find peak day & value
        for (Map.Entry<String, Double> entry : currentAggregatedData.entrySet()) {
            if (entry.getValue() > peakValue) {
                peakValue = entry.getValue();
                peakDay = entry.getKey();
            }
        }

        List<String> labels = new ArrayList<>(currentAggregatedData.keySet());
        List<Double> currentData = new ArrayList<>(currentAggregatedData.values());

        TrendResponseDto.TrendDatasetDto currentDataset = TrendResponseDto.TrendDatasetDto.builder()
                .label("Current Period")
                .data(currentData)
                .build();

        TrendResponseDto.TrendSummaryDto summary = TrendResponseDto.TrendSummaryDto.builder()
                .totalValue(totalValue)
                .percentageChange("+0%")
                .peakDay(peakDay)
                .peakValue(peakValue)
                .averageDaily(labels.size() > 0 ? totalValue / labels.size() : 0.0)
                .build();

        return TrendResponseDto.builder()
                .labels(labels)
                .datasets(Collections.singletonList(currentDataset))
                .summary(summary)
                .build();
    }

    @Override
    public List<Map<String, Object>> generateReport(String reportType, LocalDate dateFrom, LocalDate dateTo, String outlet, String category, String employee) {
        if (dateFrom == null) dateFrom = LocalDate.now().minusDays(30);
        if (dateTo == null) dateTo = LocalDate.now();

        List<Map<String, Object>> reportRows = new ArrayList<>();

        if ("Sales".equalsIgnoreCase(reportType)) {
            List<Sale> sales = saleRepository.findBySaleDateBetween(dateFrom, dateTo);
            for (Sale sale : sales) {
                // Filter by outlet (matches outletId or name)
                if (outlet != null && !outlet.trim().isEmpty() && !"All Outlets".equalsIgnoreCase(outlet)) {
                    boolean matchesOutlet = false;
                    try {
                        Long outletIdParam = Long.parseLong(outlet);
                        if (sale.getOutletId() != null && sale.getOutletId().equals(outletIdParam)) {
                            matchesOutlet = true;
                        }
                    } catch (NumberFormatException e) {
                        // Not numeric, check outlet name
                        if (sale.getOutletId() != null) {
                            Optional<Outlet> outOpt = outletRepository.findById(sale.getOutletId());
                            if (outOpt.isPresent() && outOpt.get().getName().equalsIgnoreCase(outlet)) {
                                matchesOutlet = true;
                            }
                        }
                    }
                    if (!matchesOutlet) continue;
                }

                // Filter by employee (matches cashierId UUID string, cashier username, or full name)
                if (employee != null && !employee.trim().isEmpty() && !"All Employees".equalsIgnoreCase(employee)) {
                    boolean matchesEmployee = false;
                    if (sale.getCashierId() != null) {
                        String cashierIdStr = sale.getCashierId().toString();
                        if (cashierIdStr.equalsIgnoreCase(employee)) {
                            matchesEmployee = true;
                        } else {
                            Optional<BmsAuth> cashierOpt = bmsAuthRepository.findById(IdUtil.uuidToBytes(sale.getCashierId()));
                            if (cashierOpt.isPresent()) {
                                BmsAuth cashier = cashierOpt.get();
                                String fullName = (cashier.getFirstName() != null ? cashier.getFirstName() : "") + " " +
                                                  (cashier.getLastName() != null ? cashier.getLastName() : "");
                                fullName = fullName.trim();
                                if (cashier.getUsername().equalsIgnoreCase(employee) || fullName.equalsIgnoreCase(employee)) {
                                    matchesEmployee = true;
                                }
                            }
                        }
                    }
                    if (!matchesEmployee) continue;
                }

                if (sale.getSaleItems() != null) {
                    for (SaleItem item : sale.getSaleItems()) {
                        // Filter by category
                        if (category != null && !category.trim().isEmpty() && !"All Categories".equalsIgnoreCase(category)) {
                            String itemCategory = item.getProduct() != null ? item.getProduct().getCategory() : null;
                            if (itemCategory == null || !itemCategory.equalsIgnoreCase(category)) {
                                continue;
                            }
                        }

                        Map<String, Object> row = new HashMap<>();
                        row.put("id", "INV-" + sale.getSaleId());
                        row.put("date", sale.getSaleDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                        row.put("product", item.getProduct() != null ? item.getProduct().getProductName() : "Unknown");
                        row.put("category", item.getProduct() != null ? item.getProduct().getCategory() : "N/A");
                        row.put("quantity", item.getQty());
                        row.put("unitPrice", item.getPrice());
                        row.put("total", item.getPrice().multiply(BigDecimal.valueOf(item.getQty())));
                        
                        // Resolve outlet name
                        String outletName = "Main Store";
                        if (sale.getOutletId() != null) {
                            Optional<Outlet> outOpt = outletRepository.findById(sale.getOutletId());
                            if (outOpt.isPresent()) {
                                outletName = outOpt.get().getName();
                            } else {
                                outletName = String.valueOf(sale.getOutletId());
                            }
                        }
                        row.put("outlet", outletName); 
                        reportRows.add(row);
                    }
                }
            }
        }

        return reportRows;
    }
}
