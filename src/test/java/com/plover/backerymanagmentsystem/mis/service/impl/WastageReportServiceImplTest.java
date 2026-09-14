package com.plover.backerymanagmentsystem.mis.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionBatch;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.mis.dto.WastageDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageGroupDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageRecordDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageSummaryDto;
import com.plover.backerymanagmentsystem.pos.model.OutletReturn;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnItem;
import com.plover.backerymanagmentsystem.pos.model.PaymentCategory;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.repository.OutletReturnRepository;
import com.plover.backerymanagmentsystem.pos.repository.ReturnRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentReason;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;

/**
 * Tests for {@link WastageReportServiceImpl} — covers reason mapping,
 * date validation, summary aggregation, trend bucketing, and the
 * "no sales data" path.
 *
 * <p>Lenient stubbing is used because each scenario only consumes a
 * subset of the seeded repositories; un-stubbed ones default to the
 * Mockito empty list and quietly contribute nothing to the dashboard.</p>
 */
@ExtendWith(MockitoExtension.class)
class WastageReportServiceImplTest {

    @Mock private ProductionBatchRepository productionBatchRepository;
    @Mock private ProductionPlanItemRepository productionPlanItemRepository;
    @Mock private ProductionCenterRepository productionCenterRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OutletRepository outletRepository;
    @Mock private ReturnRepository returnRepository;
    @Mock private OutletReturnRepository outletReturnRepository;
    @Mock private StockAdjustRepository stockAdjustRepository;
    @Mock private SaleRepository saleRepository;

    @InjectMocks private WastageReportServiceImpl service;

    @BeforeEach
    void defaultStubs() {
        lenient().when(productionBatchRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(productionPlanItemRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(productionCenterRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(productRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(outletRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(returnRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(outletReturnRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(stockAdjustRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(saleRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(saleRepository.findBySaleDateBetween(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    // ──────────────────────────────────────────────────────────────────
    // Reason categorisation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void normaliseReason_mapsKnownVariants() {
        assertThat(WastageReportServiceImpl.normaliseReason("Expired"))
                .isEqualTo(WastageReportServiceImpl.REASON_EXPIRED);
        assertThat(WastageReportServiceImpl.normaliseReason("damag"))
                .isEqualTo(WastageReportServiceImpl.REASON_DAMAGED);
        assertThat(WastageReportServiceImpl.normaliseReason("RETURNED"))
                .isEqualTo(WastageReportServiceImpl.REASON_RETURNED);
        assertThat(WastageReportServiceImpl.normaliseReason("other"))
                .isEqualTo(WastageReportServiceImpl.REASON_OTHER);
        assertThat(WastageReportServiceImpl.normaliseReason("garbage"))
                .isEqualTo(WastageReportServiceImpl.REASON_OTHER);
        assertThat(WastageReportServiceImpl.normaliseReason(null)).isNull();
        assertThat(WastageReportServiceImpl.normaliseReason("  ")).isNull();
    }

    @Test
    void mapStockAdjustReason_collapsesEnum() {
        assertThat(WastageReportServiceImpl.mapStockAdjustReason(StockAdjustmentReason.EXPIRED))
                .isEqualTo(WastageReportServiceImpl.REASON_EXPIRED);
        assertThat(WastageReportServiceImpl.mapStockAdjustReason(StockAdjustmentReason.DAMAGE))
                .isEqualTo(WastageReportServiceImpl.REASON_DAMAGED);
        // LOSS / COUNTING_ERROR are not real wastage causes but the helper
        // collapses them into OTHER for consistency.
        assertThat(WastageReportServiceImpl.mapStockAdjustReason(StockAdjustmentReason.LOSS))
                .isEqualTo(WastageReportServiceImpl.REASON_OTHER);
        assertThat(WastageReportServiceImpl.mapStockAdjustReason(StockAdjustmentReason.COUNTING_ERROR))
                .isEqualTo(WastageReportServiceImpl.REASON_OTHER);
        assertThat(WastageReportServiceImpl.mapStockAdjustReason(null))
                .isEqualTo(WastageReportServiceImpl.REASON_OTHER);
    }

    @Test
    void mapFreeTextReason_picksKeywords() {
        assertThat(WastageReportServiceImpl.mapFreeTextReason("Cake expired in display",
                WastageReportServiceImpl.REASON_OTHER))
                .isEqualTo(WastageReportServiceImpl.REASON_EXPIRED);
        assertThat(WastageReportServiceImpl.mapFreeTextReason("Box was damaged in transit",
                WastageReportServiceImpl.REASON_OTHER))
                .isEqualTo(WastageReportServiceImpl.REASON_DAMAGED);
        assertThat(WastageReportServiceImpl.mapFreeTextReason("Customer returned",
                WastageReportServiceImpl.REASON_OTHER))
                .isEqualTo(WastageReportServiceImpl.REASON_RETURNED);
        // Falls back to the supplied default when no keyword hits.
        assertThat(WastageReportServiceImpl.mapFreeTextReason("Burnt batch",
                WastageReportServiceImpl.REASON_OTHER))
                .isEqualTo(WastageReportServiceImpl.REASON_OTHER);
        assertThat(WastageReportServiceImpl.mapFreeTextReason(null,
                WastageReportServiceImpl.REASON_RETURNED))
                .isEqualTo(WastageReportServiceImpl.REASON_RETURNED);
    }

    // ──────────────────────────────────────────────────────────────────
    // Validation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_rejectsFutureEndDate() {
        LocalDate future = LocalDate.now().plusDays(7);
        assertThatThrownBy(() -> service.getDashboard(null, future, null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("future");
    }

    @Test
    void getDashboard_rejectsStartAfterEnd() {
        assertThatThrownBy(() -> service.getDashboard(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 1),
                null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("startDate");
    }

    // ──────────────────────────────────────────────────────────────────
    // Empty data
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_emptyData_returnsZeroSummary() {
        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                null, null, null);

        assertThat(env.getSummary().getTotalQty()).isZero();
        assertThat(env.getSummary().getTotalValue()).isNull();
        assertThat(env.getSummary().getRecordCount()).isZero();
        assertThat(env.getSummary().isSalesDataAvailable()).isFalse();
        assertThat(env.getSummary().getWastagePercent()).isNull();
        assertThat(env.getRecords()).isEmpty();
        assertThat(env.getTopProducts()).isEmpty();
        assertThat(env.getReasonBreakdown()).isEmpty();
        // Trend still spans the requested window with zero-value points.
        assertThat(env.getTrend()).isNotEmpty();
        assertThat(env.getTrend().get(0).getValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────────
    // Summary aggregation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_aggregatesProductionWastageWithSalesPercentage() {
        ProductionCenter pc = ProductionCenter.builder()
                .id(3L).centerName("Bakery East").build();
        when(productionCenterRepository.findAll()).thenReturn(List.of(pc));

        ProductionPlanItem ppi = ProductionPlanItem.builder()
                .id(99L).productId(50L).productName("Croissant").unitCost(2.5).build();
        when(productionPlanItemRepository.findAll()).thenReturn(List.of(ppi));

        Product croissant = Product.builder()
                .id(50L).productName("Croissant").unitPrice(2.5).build();
        when(productRepository.findAll()).thenReturn(List.of(croissant));

        ProductionBatch batch = ProductionBatch.builder()
                .id(1L).productionPlanItemId(99L).producedQty(100)
                .wastageQty(4).wastageReason("Expired")
                .productionCenterId(3L)
                .createdAt(LocalDateTime.of(2026, 4, 28, 9, 0))
                .build();
        when(productionBatchRepository.findAll()).thenReturn(List.of(batch));

        Sale sale = Sale.builder()
                .saleId(1)
                .saleDate(LocalDate.of(2026, 4, 28))
                .saleTime(LocalTime.NOON)
                .totalAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("100.00"))
                .outletId(7L).paymentType(PaymentCategory.CASH)
                .saleItems(Collections.emptyList())
                .build();
        when(saleRepository.findBySaleDateBetween(any(), any())).thenReturn(List.of(sale));

        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                null, null, null);

        WastageSummaryDto summary = env.getSummary();
        assertThat(summary.getTotalQty()).isEqualTo(4L);
        assertThat(summary.getTotalValue()).isEqualByComparingTo("10.00");
        assertThat(summary.isSalesDataAvailable()).isTrue();
        assertThat(summary.getSalesValue()).isEqualByComparingTo("100.00");
        // 10 / 100 = 10.00 %
        assertThat(summary.getWastagePercent()).isEqualByComparingTo("10.00");
        assertThat(summary.getRecordCount()).isEqualTo(1);

        assertThat(env.getRecords()).hasSize(1);
        assertThat(env.getRecords().get(0).getReason())
                .isEqualTo(WastageReportServiceImpl.REASON_EXPIRED);

        assertThat(env.getTopProducts()).hasSize(1);
        WastageGroupDto top = env.getTopProducts().get(0);
        assertThat(top.getLabel()).isEqualTo("Croissant");
        assertThat(top.getQty()).isEqualTo(4L);
    }

    @Test
    void getDashboard_salesDataUnavailable_setsFlagAndNullPercent() {
        // No sales repository data — but we still have a wastage source.
        ProductionBatch batch = ProductionBatch.builder()
                .id(2L).productionPlanItemId(99L).producedQty(50)
                .wastageQty(1).wastageReason("Burnt")
                .productionCenterId(3L)
                .createdAt(LocalDateTime.of(2026, 4, 20, 9, 0))
                .build();
        when(productionBatchRepository.findAll()).thenReturn(List.of(batch));
        // Plan item without a unit cost so totalValue ends up null.
        ProductionPlanItem ppi = ProductionPlanItem.builder()
                .id(99L).productId(50L).productName("Bun").build();
        when(productionPlanItemRepository.findAll()).thenReturn(List.of(ppi));

        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                null, null, null);

        WastageSummaryDto summary = env.getSummary();
        assertThat(summary.getRecordCount()).isEqualTo(1);
        assertThat(summary.getTotalQty()).isEqualTo(1L);
        assertThat(summary.getTotalValue()).isNull();
        assertThat(summary.isSalesDataAvailable()).isFalse();
        assertThat(summary.getWastagePercent()).isNull();
        assertThat(summary.getSalesValue()).isNull();
    }

    // ──────────────────────────────────────────────────────────────────
    // Stock adjustments
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_includesStockAdjustmentsAsWastage() {
        RawMaterial flour = RawMaterial.builder()
                .id(11L).materialName("Flour").unitOfMeasure("kg").unitCost(2.0).build();
        StockAdjust adj = StockAdjust.builder()
                .id(7L)
                .rawMaterial(flour)
                .changeQuantity(-3.0)
                .beforeQuantity(20.0)
                .reasonForAdjust(StockAdjustmentReason.EXPIRED)
                .createdAt(LocalDateTime.of(2026, 4, 15, 14, 0))
                .build();
        when(stockAdjustRepository.findAll()).thenReturn(List.of(adj));

        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                null, null, null);

        assertThat(env.getRecords()).hasSize(1);
        WastageRecordDto row = env.getRecords().get(0);
        assertThat(row.getReason()).isEqualTo(WastageReportServiceImpl.REASON_EXPIRED);
        assertThat(row.getProduct()).isEqualTo("Flour");
        assertThat(row.getQty()).isEqualTo(3);
        assertThat(row.getValue()).isEqualByComparingTo("6.00");
        assertThat(row.getSource())
                .isEqualTo(WastageReportServiceImpl.SOURCE_STOCK_ADJUSTMENT);
    }

    // ──────────────────────────────────────────────────────────────────
    // Trend bucketing
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_trendCoversFullSevenDayWindow() {
        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 4, 30),
                null, null, null);
        // 7 days inclusive
        assertThat(env.getTrend()).hasSize(7);
        assertThat(env.getTrend().get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 24));
        assertThat(env.getTrend().get(6).getDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        // Every point should have the human label populated.
        assertThat(env.getTrend()).allSatisfy(p -> assertThat(p.getLabel()).isNotBlank());
    }

    @Test
    void getDashboard_trendBucketsByDay() {
        ProductionCenter pc = ProductionCenter.builder().id(1L).centerName("PC").build();
        when(productionCenterRepository.findAll()).thenReturn(List.of(pc));
        ProductionPlanItem ppi = ProductionPlanItem.builder()
                .id(50L).productName("Bun").unitCost(2.0).build();
        when(productionPlanItemRepository.findAll()).thenReturn(List.of(ppi));

        ProductionBatch a = ProductionBatch.builder()
                .id(1L).productionPlanItemId(50L).producedQty(10)
                .wastageQty(2).wastageReason("Burnt")
                .productionCenterId(1L)
                .createdAt(LocalDateTime.of(2026, 4, 30, 8, 0))
                .build();
        ProductionBatch b = ProductionBatch.builder()
                .id(2L).productionPlanItemId(50L).producedQty(10)
                .wastageQty(3).wastageReason("Burnt")
                .productionCenterId(1L)
                .createdAt(LocalDateTime.of(2026, 4, 30, 12, 0))
                .build();
        when(productionBatchRepository.findAll()).thenReturn(List.of(a, b));

        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 4, 30),
                null, null, null);
        // The 30th of April should have 2 + 3 = 5 qty and value 10 (5 * 2.0).
        var lastDay = env.getTrend().get(env.getTrend().size() - 1);
        assertThat(lastDay.getDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(lastDay.getQty()).isEqualTo(5L);
        assertThat(lastDay.getValue()).isEqualByComparingTo("10.00");
    }

    // ──────────────────────────────────────────────────────────────────
    // Outlet returns — no value source
    // ──────────────────────────────────────────────────────────────────

    @Test
    void getDashboard_outletReturnsContributeQtyButFlagDataLimitation() {
        Outlet outlet = Outlet.builder().outletId(7L).name("Mall Outlet").build();
        when(outletRepository.findAll()).thenReturn(List.of(outlet));

        // Product without a unit price so the outlet-return row has no value
        // and the dataLimitation flag is set.
        Product cake = Product.builder().id(60L).productName("Cake").build();
        when(productRepository.findAll()).thenReturn(List.of(cake));

        OutletReturn ret = OutletReturn.builder()
                .id(1L).returnNoteId("ORT-001").outletId(7L)
                .reason("Damaged in transit")
                .createdAt(LocalDateTime.of(2026, 4, 25, 10, 0))
                .build();
        OutletReturnItem item = OutletReturnItem.builder()
                .id(1L).outletReturn(ret).productId(60L).qty(8).build();
        ret.setItems(List.of(item));
        when(outletReturnRepository.findAll()).thenReturn(List.of(ret));

        WastageDashboardDto env = service.getDashboard(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                null, null, null);

        assertThat(env.getRecords()).hasSize(1);
        WastageRecordDto row = env.getRecords().get(0);
        assertThat(row.getValue()).isNull();
        assertThat(row.getReason()).isEqualTo(WastageReportServiceImpl.REASON_DAMAGED);
        assertThat(env.getDataLimitation()).isEqualTo("value-not-available");
        assertThat(env.getSummary().getTotalQty()).isEqualTo(8L);
    }
}
