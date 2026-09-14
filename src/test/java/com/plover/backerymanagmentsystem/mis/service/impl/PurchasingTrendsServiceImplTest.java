package com.plover.backerymanagmentsystem.mis.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.manager.model.Brand;
import com.plover.backerymanagmentsystem.manager.model.GenericMaterial;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.mis.dto.CategoryBreakdownDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchaseRecordDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchasingDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.SupplierSpendDto;
import com.plover.backerymanagmentsystem.mis.dto.TrendPointDto;
import com.plover.backerymanagmentsystem.mis.service.PurchasingTrendsService.Granularity;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

/**
 * Tests for {@link PurchasingTrendsServiceImpl} covering granularity
 * bucketing (daily/monthly/yearly), this-year-vs-last-year computation,
 * future-date rejection, supplier and category filtering, the no-op
 * outlet filter, invalid-granularity rejection, summary aggregation,
 * and pending-PO derivation.
 *
 * <p>Lenient stubbing is used because each scenario only exercises a
 * subset of the seeded repositories.</p>
 */
@ExtendWith(MockitoExtension.class)
class PurchasingTrendsServiceImplTest {

    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private GrnRepository grnRepository;
    @Mock private SupplierRepository supplierRepository;

    @InjectMocks private PurchasingTrendsServiceImpl service;

    private static final Long SUP_FRESH = 1L;
    private static final Long SUP_DAIRY = 2L;

    @BeforeEach
    void defaultStubs() {
        lenient().when(purchaseOrderRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(grnRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(supplierRepository.findAll()).thenReturn(Collections.emptyList());
    }

    // ──────────────────────────────────────────────────────────────────
    // Fixture helpers
    // ──────────────────────────────────────────────────────────────────

    private Supplier supplier(Long id, String name) {
        return Supplier.builder()
                .supplierId(id)
                .name(name)
                .contactNumber("+94 11 000 " + id)
                .email(name.toLowerCase().replace(" ", "") + "@test.lk")
                .address("Test Lane, Colombo")
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
    }

    private RawMaterial rawMaterial(Long id, String name, String unit, String categoryName) {
        GenericMaterial gm = GenericMaterial.builder()
                .id(id * 100)
                .name(name + " Generic")
                .unitOfMeasure(unit)
                .category(categoryName)
                .build();
        Brand brand = Brand.builder()
                .id(id * 10)
                .name(name + " Brand")
                .genericMaterial(gm)
                .build();
        return RawMaterial.builder()
                .id(id)
                .materialName(name)
                .materialCode("RM-" + id)
                .unitOfMeasure(unit)
                .unitCost(10.0)
                .currentStock(100.0)
                .isActive(true)
                .brand(brand)
                .build();
    }

    private PurchaseOrder po(Long id, Long supplierId, LocalDate date, String status,
                             BigDecimal totalCost, List<PurchaseOrderItem> items) {
        PurchaseOrder po = PurchaseOrder.builder()
                .poId(id)
                .supplierId(supplierId)
                .estimatedDeliveryDate(date)
                .status(status)
                .totalCost(totalCost)
                .numberOfItems(items != null ? items.size() : 0)
                .purchaseOrderItems(items)
                .build();
        if (items != null) {
            for (PurchaseOrderItem it : items) it.setPurchaseOrder(po);
        }
        return po;
    }

    private PurchaseOrderItem item(Long id, RawMaterial rm, int qty, BigDecimal totalCost) {
        return PurchaseOrderItem.builder()
                .poiId(id)
                .rawMaterialId(rm.getId().intValue())
                .rawMaterial(rm)
                .requiredQty(qty)
                .receivedQty(qty)
                .estimatedCost(totalCost)
                .actualCost(totalCost)
                .unitOfMeasure(rm.getUnitOfMeasure())
                .build();
    }

    private Grn grn(Long id, Long poId, Long supplierId, LocalDate date, GrnStatus status) {
        return Grn.builder()
                .grnId(id)
                .poId(poId)
                .supplierId(supplierId)
                .receivedDate(date != null ? date.atStartOfDay() : null)
                .grnStatus(status)
                .total(BigDecimal.valueOf(100))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // Validation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_rejectsFutureEndDate() {
        LocalDate future = LocalDate.now().plusDays(3);
        assertThatThrownBy(() -> service.getDashboard(null, future, null, null, null, Granularity.MONTHLY))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("future");
    }

    @Test
    void getDashboard_rejectsStartAfterEnd() {
        assertThatThrownBy(() -> service.getDashboard(
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1),
                null, null, null, Granularity.MONTHLY))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("startDate");
    }

    @Test
    void granularity_fromRejectsUnknownValues() {
        assertThatThrownBy(() -> Granularity.from("hourly"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid granularity");
        // Defaults / known values still work.
        assertThat(Granularity.from(null)).isEqualTo(Granularity.MONTHLY);
        assertThat(Granularity.from("  ")).isEqualTo(Granularity.MONTHLY);
        assertThat(Granularity.from("daily")).isEqualTo(Granularity.DAILY);
        assertThat(Granularity.from("MONTHLY")).isEqualTo(Granularity.MONTHLY);
        assertThat(Granularity.from("yearly")).isEqualTo(Granularity.YEARLY);
    }

    // ──────────────────────────────────────────────────────────────────
    // Empty / outlet no-op
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_emptyData_returnsZeroSummaryAndDefaultTrendShape() {
        PurchasingDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.MONTHLY);

        assertThat(env.getSummary().getTotalPos()).isZero();
        assertThat(env.getSummary().getTotalSpend()).isEqualByComparingTo("0.00");
        assertThat(env.getSummary().getAvgOrderValue()).isEqualByComparingTo("0");
        assertThat(env.getSummary().getPendingPos()).isZero();
        assertThat(env.getRecords()).isEmpty();
        assertThat(env.getCategoryBreakdown()).isEmpty();
        assertThat(env.getSupplierRanking()).isEmpty();
        assertThat(env.getTrend()).isNotEmpty();
        assertThat(env.getTrend()).allSatisfy(p -> {
            assertThat(p.getThisYear()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(p.getLastYear()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(env.getDataLimitation()).isNull();
    }

    @Test
    void getDashboard_outletFilterFlagsLimitation() {
        PurchasingDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                null, null, /* outletId */ 99L, Granularity.MONTHLY);

        assertThat(env.getDataLimitation()).isEqualTo("outlet-filter-noop");
        assertThat(env.getDataLimitationReason()).contains("centrally");
    }

    // ──────────────────────────────────────────────────────────────────
    // Summary aggregation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_aggregatesPoSpendAndPendingCount() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms"), supplier(SUP_DAIRY, "Metro Dairy")));

        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");
        RawMaterial milk = rawMaterial(11L, "Milk", "L", "Dairy");

        PurchaseOrder po1 = po(101L, SUP_FRESH, LocalDate.of(2026, 4, 28), "APPROVED",
                new BigDecimal("6000"),
                List.of(item(1L, chicken, 150, new BigDecimal("6000"))));
        PurchaseOrder po2 = po(102L, SUP_DAIRY, LocalDate.of(2026, 4, 27), "APPROVED",
                new BigDecimal("4200"),
                List.of(item(2L, milk, 300, new BigDecimal("4200"))));
        PurchaseOrder po3 = po(103L, SUP_FRESH, LocalDate.of(2026, 4, 26), "PENDING",
                new BigDecimal("2400"),
                List.of(item(3L, chicken, 60, new BigDecimal("2400"))));
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(po1, po2, po3));

        // PO1 received fully, PO2 partial, PO3 has no GRN → still pending.
        when(grnRepository.findAll()).thenReturn(List.of(
                grn(901L, 101L, SUP_FRESH, LocalDate.of(2026, 4, 28), GrnStatus.RECEIVED),
                grn(902L, 102L, SUP_DAIRY, LocalDate.of(2026, 4, 27), GrnStatus.PARTIAL)));

        PurchasingDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.MONTHLY);

        assertThat(env.getSummary().getTotalPos()).isEqualTo(3L);
        assertThat(env.getSummary().getTotalSpend()).isEqualByComparingTo("12600.00");
        // 12600 / 3 = 4200.00
        assertThat(env.getSummary().getAvgOrderValue()).isEqualByComparingTo("4200.00");
        // Two non-RECEIVED POs: PARTIAL + Pending(no GRN) = 2.
        assertThat(env.getSummary().getPendingPos()).isEqualTo(2L);

        assertThat(env.getRecords()).hasSize(3);
        assertThat(env.getRecords()).extracting(PurchaseRecordDto::getStatus)
                .containsExactlyInAnyOrder("Received", "Partial", "Pending");
    }

    // ──────────────────────────────────────────────────────────────────
    // Filters
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_supplierFilterRestrictsRows() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms"), supplier(SUP_DAIRY, "Metro Dairy")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");
        RawMaterial milk = rawMaterial(11L, "Milk", "L", "Dairy");

        when(purchaseOrderRepository.findAll()).thenReturn(List.of(
                po(101L, SUP_FRESH, LocalDate.of(2026, 4, 28), "APPROVED",
                        new BigDecimal("6000"),
                        List.of(item(1L, chicken, 150, new BigDecimal("6000")))),
                po(102L, SUP_DAIRY, LocalDate.of(2026, 4, 27), "APPROVED",
                        new BigDecimal("4200"),
                        List.of(item(2L, milk, 300, new BigDecimal("4200"))))));

        PurchasingDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                SUP_FRESH, null, null, Granularity.MONTHLY);

        assertThat(env.getRecords()).hasSize(1);
        assertThat(env.getRecords().get(0).getSupplier()).isEqualTo("Fresh Farms");
        assertThat(env.getSummary().getTotalSpend()).isEqualByComparingTo("6000.00");
        assertThat(env.getSupplierRanking()).hasSize(1);
    }

    @Test
    void getDashboard_categoryFilterRestrictsRowsAndUncategorisedFallback() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");
        // RM with no brand at all → category proxy returns null → bucketed as
        // "Uncategorized" both in the breakdown AND rejected by a category filter
        // for any other label.
        RawMaterial mystery = RawMaterial.builder()
                .id(20L).materialName("Mystery Spice").materialCode("RM-20")
                .unitOfMeasure("g").unitCost(50.0).currentStock(10.0).isActive(true)
                .build();

        PurchaseOrder po = po(101L, SUP_FRESH, LocalDate.of(2026, 4, 28), "APPROVED",
                new BigDecimal("8000"),
                List.of(
                        item(1L, chicken, 100, new BigDecimal("4000")),
                        item(2L, mystery, 50, new BigDecimal("4000"))));
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(po));

        PurchasingDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                null, "Poultry & Meat", null, Granularity.MONTHLY);

        assertThat(env.getRecords()).hasSize(1);
        assertThat(env.getRecords().get(0).getCategory()).isEqualTo("Poultry & Meat");

        // No filter → both rows returned, mystery item bucketed as Uncategorized.
        PurchasingDashboardDto unfiltered = service.getDashboard(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.MONTHLY);
        assertThat(unfiltered.getRecords()).hasSize(2);
        assertThat(unfiltered.getCategoryBreakdown())
                .extracting(CategoryBreakdownDto::getLabel)
                .contains("Poultry & Meat", "Uncategorized");
    }

    // ──────────────────────────────────────────────────────────────────
    // Trend bucketing
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_dailyTrendCoversSevenDayWindowAndComparesPriorYear() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");

        // Two POs: one on 2026-04-30 (this year), one on 2025-04-30 (last
        // year) — same day-of-year so the daily lookback should pair them up.
        PurchaseOrder thisYear = po(101L, SUP_FRESH, LocalDate.of(2026, 4, 30), "APPROVED",
                new BigDecimal("5000"),
                List.of(item(1L, chicken, 100, new BigDecimal("5000"))));
        PurchaseOrder lastYear = po(102L, SUP_FRESH, LocalDate.of(2025, 4, 30), "APPROVED",
                new BigDecimal("3000"),
                List.of(item(2L, chicken, 80, new BigDecimal("3000"))));
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(thisYear, lastYear));

        PurchasingDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.DAILY);

        assertThat(env.getTrend()).hasSize(7);
        TrendPointDto last = env.getTrend().get(env.getTrend().size() - 1);
        assertThat(last.getLabel()).isEqualTo("Apr 30");
        assertThat(last.getThisYear()).isEqualByComparingTo("5000.00");
        assertThat(last.getLastYear()).isEqualByComparingTo("3000.00");
    }

    @Test
    void getTrend_monthlyHasEightBucketsAndYearOverYearComparison() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");

        PurchaseOrder april2026 = po(101L, SUP_FRESH, LocalDate.of(2026, 4, 15), "APPROVED",
                new BigDecimal("12000"),
                List.of(item(1L, chicken, 100, new BigDecimal("12000"))));
        PurchaseOrder april2025 = po(102L, SUP_FRESH, LocalDate.of(2025, 4, 15), "APPROVED",
                new BigDecimal("8000"),
                List.of(item(2L, chicken, 80, new BigDecimal("8000"))));
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(april2026, april2025));

        List<TrendPointDto> series = service.getTrend(
                null, LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.MONTHLY);

        assertThat(series).hasSize(8);
        // Most recent bucket is April; expect this year = 12000, last year = 8000.
        TrendPointDto april = series.get(series.size() - 1);
        assertThat(april.getLabel()).isEqualTo("Apr");
        assertThat(april.getThisYear()).isEqualByComparingTo("12000.00");
        assertThat(april.getLastYear()).isEqualByComparingTo("8000.00");
    }

    @Test
    void getTrend_yearlyComparesBucketAgainstPreviousYear() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");

        PurchaseOrder y2024 = po(101L, SUP_FRESH, LocalDate.of(2024, 6, 1), "APPROVED",
                new BigDecimal("80000"),
                List.of(item(1L, chicken, 800, new BigDecimal("80000"))));
        PurchaseOrder y2025 = po(102L, SUP_FRESH, LocalDate.of(2025, 6, 1), "APPROVED",
                new BigDecimal("100000"),
                List.of(item(2L, chicken, 1000, new BigDecimal("100000"))));
        PurchaseOrder y2026 = po(103L, SUP_FRESH, LocalDate.of(2026, 4, 1), "APPROVED",
                new BigDecimal("60000"),
                List.of(item(3L, chicken, 600, new BigDecimal("60000"))));
        when(purchaseOrderRepository.findAll()).thenReturn(List.of(y2024, y2025, y2026));

        List<TrendPointDto> series = service.getTrend(
                null, LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.YEARLY);

        assertThat(series).hasSize(5); // YEARLY_BUCKETS
        // Bucket labels are simple year strings.
        assertThat(series).extracting(TrendPointDto::getLabel)
                .containsExactly("2022", "2023", "2024", "2025", "2026");

        TrendPointDto p2025 = series.get(3);
        assertThat(p2025.getThisYear()).isEqualByComparingTo("100000.00");
        // 2025's lastYear bucket should equal 2024's spend.
        assertThat(p2025.getLastYear()).isEqualByComparingTo("80000.00");

        TrendPointDto p2026 = series.get(4);
        assertThat(p2026.getThisYear()).isEqualByComparingTo("60000.00");
        assertThat(p2026.getLastYear()).isEqualByComparingTo("100000.00");
    }

    // ──────────────────────────────────────────────────────────────────
    // Records pagination + supplier ranking ordering
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getRecords_paginatesAndSearchesAcrossPoSupplierProduct() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms"), supplier(SUP_DAIRY, "Metro Dairy")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");
        RawMaterial milk = rawMaterial(11L, "Milk", "L", "Dairy");

        when(purchaseOrderRepository.findAll()).thenReturn(List.of(
                po(101L, SUP_FRESH, LocalDate.of(2026, 4, 28), "APPROVED",
                        new BigDecimal("6000"),
                        List.of(item(1L, chicken, 150, new BigDecimal("6000")))),
                po(102L, SUP_DAIRY, LocalDate.of(2026, 4, 27), "APPROVED",
                        new BigDecimal("4200"),
                        List.of(item(2L, milk, 300, new BigDecimal("4200"))))));

        Page<PurchaseRecordDto> page = service.getRecords(null, null, null, null, null,
                "milk", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getProduct()).isEqualTo("Milk");

        Page<PurchaseRecordDto> all = service.getRecords(null, null, null, null, null,
                null, PageRequest.of(0, 1));
        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(all.getContent()).hasSize(1); // page size honoured
    }

    @Test
    void getDashboard_supplierRankingSortedByDescendingSpend() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(SUP_FRESH, "Fresh Farms"), supplier(SUP_DAIRY, "Metro Dairy")));
        RawMaterial chicken = rawMaterial(10L, "Chicken", "kg", "Poultry & Meat");
        RawMaterial milk = rawMaterial(11L, "Milk", "L", "Dairy");

        when(purchaseOrderRepository.findAll()).thenReturn(List.of(
                // Dairy spends more in total ($8000) than Fresh ($6000).
                po(101L, SUP_FRESH, LocalDate.of(2026, 4, 28), "APPROVED",
                        new BigDecimal("6000"),
                        List.of(item(1L, chicken, 150, new BigDecimal("6000")))),
                po(102L, SUP_DAIRY, LocalDate.of(2026, 4, 27), "APPROVED",
                        new BigDecimal("8000"),
                        List.of(item(2L, milk, 300, new BigDecimal("8000"))))));

        PurchasingDashboardDto env = service.getDashboard(
                null, LocalDate.of(2026, 4, 30),
                null, null, null, Granularity.MONTHLY);

        assertThat(env.getSupplierRanking()).hasSize(2);
        SupplierSpendDto first = env.getSupplierRanking().get(0);
        assertThat(first.getSupplierName()).isEqualTo("Metro Dairy");
        assertThat(first.getTotalSpend()).isEqualByComparingTo("8000.00");
    }
}
