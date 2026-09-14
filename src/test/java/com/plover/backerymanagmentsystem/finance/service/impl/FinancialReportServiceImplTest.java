package com.plover.backerymanagmentsystem.finance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.plover.backerymanagmentsystem.finance.dto.ReportEnvelopeDto;
import com.plover.backerymanagmentsystem.finance.dto.ReportRowDto;
import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;
import com.plover.backerymanagmentsystem.finance.repository.SupplierPaymentRepository;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionBatch;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository;
import com.plover.backerymanagmentsystem.pos.model.PaymentCategory;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.model.SaleItem;
import com.plover.backerymanagmentsystem.pos.model.DayProduction;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosing;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosingItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterialRequirement;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialRequirementRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

/**
 * Tests for {@link FinancialReportServiceImpl}.
 *
 * <p>Lenient stubbing is used because each report family only consumes a
 * subset of the seeded repositories; un-stubbed repositories return Mockito
 * default empty lists which keep the rest of the report quietly empty.</p>
 */
@ExtendWith(MockitoExtension.class)
class FinancialReportServiceImplTest {

    @Mock private SupplierPaymentRepository paymentRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private GrnRepository grnRepository;
    @Mock private GrnItemRepository grnItemRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OutletRepository outletRepository;
    @Mock private ProductionBatchRepository productionBatchRepository;
    @Mock private ProductionPlanItemRepository productionPlanItemRepository;
    @Mock private ProductionCenterRepository productionCenterRepository;
    @Mock private RawMaterialRepository rawMaterialRepository;
    @Mock private StockAdjustRepository stockAdjustRepository;
    @Mock private BillOfMaterialRepository billOfMaterialRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;
    @Mock private DayProductionRepository dayProductionRepository;
    @Mock private DayProductionItemRepository dayProductionItemRepository;
    @Mock private DayEndClosingRepository dayEndClosingRepository;
    @Mock private DayEndClosingItemRepository dayEndClosingItemRepository;
    @Mock private RawMaterialRequirementRepository rawMaterialRequirementRepository;

    @InjectMocks private FinancialReportServiceImpl service;

    @BeforeEach
    void defaultStubs() {
        // Most reports walk findAll() lookups; default to empty so individual
        // tests can opt-in to the data they need.
        lenient().when(supplierRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(grnRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(saleRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(saleRepository.findBySaleDateBetween(any(), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(productRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(outletRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(productionBatchRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(productionPlanItemRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(productionCenterRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(rawMaterialRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(rawMaterialRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        lenient().when(stockAdjustRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc())
                .thenReturn(Collections.emptyList());
        lenient().when(billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(dayProductionRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(dayEndClosingRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(rawMaterialRequirementRepository.findAll()).thenReturn(Collections.emptyList());
    }

    // ──────────────────────────────────────────────────────────────────
    // Payments
    // ──────────────────────────────────────────────────────────────────

    @Test
    void paymentSummary_includesRowsFromSupplierPayments() {
        Supplier sup = supplier(1L, "Acme Foods");
        when(supplierRepository.findAll()).thenReturn(List.of(sup));

        SupplierPayment p = SupplierPayment.builder()
                .paymentId(101L)
                .paymentRef("PMT-2026-001")
                .paymentDate(LocalDate.of(2026, 4, 5))
                .supplierId(1L)
                .amount(new BigDecimal("250.00"))
                .paymentMethod("BANK_TRANSFER")
                .status("CLEARED")
                .build();
        when(paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc())
                .thenReturn(List.of(p));

        Grn grn = grn(11L, 1L, LocalDate.of(2026, 4, 1), new BigDecimal("400.00"));
        when(grnRepository.findAll()).thenReturn(List.of(grn));

        ReportEnvelopeDto env = service.getPaymentSummary("Monthly", null, null);

        assertThat(env.getReportType()).isEqualTo("payments");
        assertThat(env.isDataUnavailable()).isFalse();
        assertThat(env.getRows()).hasSize(1);

        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getId()).isEqualTo("PMT-101");
        assertThat(row.getSupplier()).isEqualTo("Acme Foods");
        assertThat(row.getRef()).isEqualTo("PMT-2026-001");
        assertThat(row.getPaid()).isEqualByComparingTo("250.00");
        assertThat(row.getTotalInvoiced()).isEqualByComparingTo("400.00");
        assertThat(row.getOutstanding()).isEqualByComparingTo("150.00");
        assertThat(row.getStatus()).isEqualTo("Cleared");

        assertThat(env.getTotals()).containsKeys("invoiced", "paid", "outstanding");
    }

    @Test
    void paymentSummary_dateRangeFiltersOutOldPayments() {
        SupplierPayment recent = SupplierPayment.builder()
                .paymentId(1L).paymentRef("PMT-1")
                .paymentDate(LocalDate.of(2026, 4, 5))
                .supplierId(1L).amount(new BigDecimal("10")).status("PENDING")
                .build();
        SupplierPayment old = SupplierPayment.builder()
                .paymentId(2L).paymentRef("PMT-2")
                .paymentDate(LocalDate.of(2025, 1, 1))
                .supplierId(1L).amount(new BigDecimal("20")).status("PENDING")
                .build();
        when(paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc())
                .thenReturn(List.of(recent, old));

        ReportEnvelopeDto env = service.getPaymentSummary(
                "Custom",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31));

        assertThat(env.getRows()).hasSize(1);
        assertThat(env.getRows().get(0).getRef()).isEqualTo("PMT-1");
    }

    // ──────────────────────────────────────────────────────────────────
    // Sales
    // ──────────────────────────────────────────────────────────────────

    @Test
    void salesByProduct_aggregatesSaleItems() {
        Outlet outlet = Outlet.builder().outletId(7L).name("Main Outlet").build();
        when(outletRepository.findAll()).thenReturn(List.of(outlet));

        Product chicken = Product.builder()
                .id(50L).productName("Grilled Chicken").unitPrice(10.0).build();
        SaleItem item = SaleItem.builder()
                .saleItemId(1).productId(50L).qty(3)
                .price(new BigDecimal("30.00")).appliedDiscount(BigDecimal.ZERO)
                .product(chicken)
                .build();
        Sale sale = Sale.builder()
                .saleId(1).saleDate(LocalDate.of(2026, 4, 30)).saleTime(LocalTime.NOON)
                .totalAmount(new BigDecimal("30.00")).discountAmount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("30.00"))
                .outletId(7L).paymentType(PaymentCategory.CASH)
                .saleItems(List.of(item))
                .build();
        when(saleRepository.findAll()).thenReturn(List.of(sale));
        // salesByProduct now pre-loads all products via findAll() to avoid
        // lazy-loading failures; provide the product so the index resolves it.
        when(productRepository.findAll()).thenReturn(List.of(chicken));

        ReportEnvelopeDto env = service.getSalesReport(
                "byProduct", null, null, null);

        assertThat(env.getRows()).hasSize(1);
        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getProduct()).isEqualTo("Grilled Chicken");
        assertThat(row.getOutlet()).isEqualTo("Main Outlet");
        assertThat(row.getSales()).isEqualByComparingTo("30.00");
        assertThat(row.getNet()).isEqualByComparingTo("30.00");
    }


    @Test
    void salesByInterval_aggregatesPerDay() {
        Sale s1 = Sale.builder()
                .saleId(1).saleDate(LocalDate.of(2026, 4, 30)).saleTime(LocalTime.NOON)
                .totalAmount(new BigDecimal("100")).discountAmount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("100"))
                .outletId(1L).paymentType(PaymentCategory.CASH)
                .saleItems(Collections.emptyList())
                .build();
        Sale s2 = Sale.builder()
                .saleId(2).saleDate(LocalDate.of(2026, 4, 30)).saleTime(LocalTime.NOON)
                .totalAmount(new BigDecimal("50")).discountAmount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("50"))
                .outletId(1L).paymentType(PaymentCategory.CASH)
                .saleItems(Collections.emptyList())
                .build();
        when(saleRepository.findAll()).thenReturn(List.of(s1, s2));

        ReportEnvelopeDto env = service.getSalesReport(
                "byInterval", null, null, null);

        assertThat(env.getRows()).hasSize(1);
        assertThat(env.getRows().get(0).getSales()).isEqualByComparingTo("150");
    }

    // ──────────────────────────────────────────────────────────────────
    // Wastage
    // ──────────────────────────────────────────────────────────────────

    @Test
    void wastageByProduct_includesBatchesWithWastage() {
        ProductionCenter pc = ProductionCenter.builder()
                .id(3L).centerName("Bakery East").build();
        when(productionCenterRepository.findAll()).thenReturn(List.of(pc));

        ProductionPlanItem ppi = ProductionPlanItem.builder()
                .id(99L).productId(50L).productName("Croissant")
                .unitCost(2.5).build();
        when(productionPlanItemRepository.findAll()).thenReturn(List.of(ppi));

        Product croissant = Product.builder()
                .id(50L).productName("Croissant").unitPrice(2.5).build();
        when(productRepository.findAll()).thenReturn(List.of(croissant));

        ProductionBatch batchWithWaste = ProductionBatch.builder()
                .id(1L).productionPlanItemId(99L).producedQty(100)
                .wastageQty(5).wastageReason("Burnt")
                .productionCenterId(3L)
                .createdAt(LocalDateTime.of(2026, 4, 30, 9, 0))
                .build();
        ProductionBatch batchNoWaste = ProductionBatch.builder()
                .id(2L).productionPlanItemId(99L).producedQty(100)
                .wastageQty(0).wastageReason(null)
                .productionCenterId(3L)
                .createdAt(LocalDateTime.of(2026, 4, 30, 10, 0))
                .build();
        when(productionBatchRepository.findAll())
                .thenReturn(List.of(batchWithWaste, batchNoWaste));

        ReportEnvelopeDto env = service.getWastageReport(
                "byProduct", null, null, null);

        assertThat(env.getRows()).hasSize(1);
        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getProduct()).isEqualTo("Croissant");
        assertThat(row.getOutlet()).isEqualTo("Bakery East");
        assertThat(row.getQty()).isEqualTo(5);
        assertThat(row.getReason()).isEqualTo("Burnt");
        assertThat(row.getCostPerUnit()).isEqualByComparingTo("2.5");
        assertThat(row.getTotalCost()).isEqualByComparingTo("12.5");
    }

    @Test
    void rawMaterialCosts_aggregatesGrnItems() {
        Supplier sup = supplier(2L, "Mill Co");
        when(supplierRepository.findAll()).thenReturn(List.of(sup));

        Grn grn = grn(15L, 2L, LocalDate.of(2026, 4, 28), new BigDecimal("0"));
        when(grnRepository.findAll()).thenReturn(List.of(grn));

        GrnItem item = GrnItem.builder()
                .grnItemId(1L).grnId(15L).rawMaterialId(77L)
                .receivedQuantity(new BigDecimal("10"))
                .pricePerUnit(new BigDecimal("4.00"))
                .uom("kg")
                .build();
        when(grnItemRepository.findByGrnId(15L)).thenReturn(List.of(item));

        ReportEnvelopeDto env = service.getWastageReport(
                "rawMaterialCosts", null, null, null);

        assertThat(env.getSubType()).isEqualTo("rawMaterialCosts");
        assertThat(env.getRows()).hasSize(1);
        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getSupplier()).isEqualTo("Mill Co");
        assertThat(row.getCostPerUnit()).isEqualByComparingTo("4.00");
        assertThat(row.getTotalCost()).isEqualByComparingTo("40.00");
    }

    // ──────────────────────────────────────────────────────────────────
    // Profitability — data unavailable
    // ──────────────────────────────────────────────────────────────────

    @Test
    void profitability_returnsDataUnavailable() {
        ReportEnvelopeDto env = service.getProfitabilityReport("byProduct", null, null);
        assertThat(env.isDataUnavailable()).isTrue();
        assertThat(env.getRows()).isEmpty();
        assertThat(env.getUnavailableReason()).isNotBlank();
    }

    // ──────────────────────────────────────────────────────────────────
    // Staff meals
    // ──────────────────────────────────────────────────────────────────

    @Test
    void staffMeals_onlyIncludesFreeMealSales() {
        Sale free = Sale.builder()
                .saleId(1).saleDate(LocalDate.of(2026, 4, 30)).saleTime(LocalTime.NOON)
                .totalAmount(new BigDecimal("12.50")).discountAmount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("12.50"))
                .outletId(1L).paymentType(PaymentCategory.FREE_MEAL)
                .saleItems(Collections.emptyList())
                .build();
        Sale paid = Sale.builder()
                .saleId(2).saleDate(LocalDate.of(2026, 4, 30)).saleTime(LocalTime.NOON)
                .totalAmount(new BigDecimal("20.00")).discountAmount(BigDecimal.ZERO)
                .finalTotal(new BigDecimal("20.00"))
                .outletId(1L).paymentType(PaymentCategory.CASH)
                .saleItems(Collections.emptyList())
                .build();
        when(saleRepository.findAll()).thenReturn(List.of(free, paid));

        ReportEnvelopeDto env = service.getStaffMealReport(null, null);

        assertThat(env.getRows()).hasSize(1);
        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getId()).isEqualTo("FM-1");
        assertThat(row.getReason()).isEqualTo("Staff Meal");
        assertThat(row.getTotalCost()).isEqualByComparingTo("12.50");
    }

    // ──────────────────────────────────────────────────────────────────
    // Stock movement
    // ──────────────────────────────────────────────────────────────────

    @Test
    void stockMovement_outflowReturnsEmptyWhenNoData() {
        ReportEnvelopeDto env = service.getStockMovementReport(
                "outflow", null, null);
        assertThat(env.isDataUnavailable()).isFalse();
        assertThat(env.getRows()).isEmpty();
    }

    @Test
    void stockMovement_inflowBuildsFromGrnItems() {
        Supplier sup = supplier(1L, "Mill Co");
        when(supplierRepository.findAll()).thenReturn(List.of(sup));

        Grn grn = grn(20L, 1L, LocalDate.of(2026, 4, 30), new BigDecimal("100"));
        when(grnRepository.findAll()).thenReturn(List.of(grn));

        GrnItem item = GrnItem.builder()
                .grnItemId(2L).grnId(20L).rawMaterialId(33L)
                .receivedQuantity(new BigDecimal("5"))
                .pricePerUnit(new BigDecimal("2.00"))
                .uom("kg").build();
        when(grnItemRepository.findByGrnId(20L)).thenReturn(List.of(item));

        ReportEnvelopeDto env = service.getStockMovementReport(
                "inflow", null, null);

        assertThat(env.getRows()).hasSize(1);
        assertThat(env.getRows().get(0).getReason()).isEqualTo("Inflow");
        assertThat(env.getRows().get(0).getTotalCost()).isEqualByComparingTo("10.00");
    }

    // ──────────────────────────────────────────────────────────────────
    // Purchase price
    // ──────────────────────────────────────────────────────────────────

    @Test
    void purchasePrice_computesChangeVsPrevious() {
        Supplier sup = supplier(1L, "Acme");
        when(supplierRepository.findAll()).thenReturn(List.of(sup));

        Grn first = grn(1L, 1L, LocalDate.of(2026, 4, 1), new BigDecimal("0"));
        Grn second = grn(2L, 1L, LocalDate.of(2026, 4, 15), new BigDecimal("0"));
        when(grnRepository.findAll()).thenReturn(List.of(first, second));

        GrnItem firstItem = GrnItem.builder()
                .grnItemId(1L).grnId(1L).rawMaterialId(77L)
                .receivedQuantity(new BigDecimal("1"))
                .pricePerUnit(new BigDecimal("10.00"))
                .uom("kg").build();
        GrnItem secondItem = GrnItem.builder()
                .grnItemId(2L).grnId(2L).rawMaterialId(77L)
                .receivedQuantity(new BigDecimal("1"))
                .pricePerUnit(new BigDecimal("12.00"))
                .uom("kg").build();
        when(grnItemRepository.findByGrnId(1L)).thenReturn(List.of(firstItem));
        when(grnItemRepository.findByGrnId(2L)).thenReturn(List.of(secondItem));

        ReportEnvelopeDto env = service.getPurchasePriceReport(
                "priceChange", null, null);

        assertThat(env.getRows()).hasSize(2);
        // The row with non-null change should have +20.00% (12 vs 10).
        ReportRowDto changed = env.getRows().stream()
                .filter(r -> r.getChange() != null)
                .findFirst().orElseThrow();
        assertThat(changed.getChange()).isEqualByComparingTo("20.00");
    }

    // ──────────────────────────────────────────────────────────────────
    // Variance — data unavailable
    // ──────────────────────────────────────────────────────────────────

    @Test
    void variance_returnsDataUnavailable() {
        ReportEnvelopeDto env = service.getVarianceReport("priceVariance", null, null);
        assertThat(env.isDataUnavailable()).isTrue();
        assertThat(env.getRows()).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────
    // Sub-type normalisation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void normalizeSubType_handlesMixedInputs() {
        assertThat(FinancialReportServiceImpl.normalizeSubType("By Product"))
                .isEqualTo("byProduct");
        assertThat(FinancialReportServiceImpl.normalizeSubType("byProduct"))
                .isEqualTo("byProduct");
        assertThat(FinancialReportServiceImpl.normalizeSubType("price-change"))
                .isEqualTo("priceChange");
        assertThat(FinancialReportServiceImpl.normalizeSubType(""))
                .isEqualTo("");
        assertThat(FinancialReportServiceImpl.normalizeSubType(null))
                .isEqualTo("");
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    private static Supplier supplier(Long id, String name) {
        return Supplier.builder()
                .supplierId(id).name(name)
                .contactNumber("+94 11 000 " + id)
                .email("info@example.lk")
                .address(id + " Mill Lane")
                .build();
    }

    private static Grn grn(Long id, Long supplierId, LocalDate date, BigDecimal total) {
        return Grn.builder()
                .grnId(id)
                .receivedDate(date.atStartOfDay())
                .poId(100L + id)
                .supplierId(supplierId)
                .total(total)
                .grnStatus(GrnStatus.RECEIVED)
                .invoiceNumber("INV-" + id)
                .build();
    }

    @Test
    void stockMovement_inflowIncludesDayProduction() {
        DayProduction dp = DayProduction.builder()
                .productionId(200)
                .orderedDate(LocalDate.of(2026, 7, 4))
                .isActive(true)
                .build();
        Product product = Product.builder()
                .id(999L)
                .productName("Chocolate Donut")
                .unitPrice(1.50)
                .build();
        DayProductionItem dpi = DayProductionItem.builder()
                .dayProductionItemId(300)
                .product(product)
                .dayProduction(dp)
                .receivedQty(10)
                .build();

        when(dayProductionRepository.findAll()).thenReturn(List.of(dp));
        when(dayProductionItemRepository.findByProductionIdWithProduct(200)).thenReturn(List.of(dpi));

        ReportEnvelopeDto env = service.getStockMovementReport("inflow", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5));

        assertThat(env.getRows()).hasSize(1);
        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getProduct()).isEqualTo("Chocolate Donut");
        assertThat(row.getSupplier()).isEqualTo("Internal Production");
        assertThat(row.getQty()).isEqualTo(10);
        assertThat(row.getCostPerUnit()).isEqualByComparingTo("1.50");
        assertThat(row.getTotalCost()).isEqualByComparingTo("15.00");
    }

    @Test
    void stockMovement_outflowIncludesMaterialIssuances() {
        ProductionPlan plan = ProductionPlan.builder().id(10L).planName("Plan A").build();
        ProductionOrder order = ProductionOrder.builder()
                .id(1L)
                .productionPlan(plan)
                .orderNumber("PO-01")
                .orderDate(LocalDateTime.of(2026, 7, 4, 10, 0))
                .build();
        RawMaterial material = RawMaterial.builder()
                .id(55L)
                .materialName("Wheat Flour")
                .unitCost(0.80)
                .unitOfMeasure("kg")
                .build();
        RawMaterialRequirement req = RawMaterialRequirement.builder()
                .id(1L)
                .productionOrder(order)
                .rawMaterial(material)
                .requiredQuantity(15.0)
                .unitOfMeasure("kg")
                .unitCost(0.80)
                .totalCost(12.0)
                .build();

        when(rawMaterialRequirementRepository.findAll()).thenReturn(List.of(req));

        ReportEnvelopeDto env = service.getStockMovementReport("outflow", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5));

        assertThat(env.getRows()).hasSize(1);
        ReportRowDto row = env.getRows().get(0);
        assertThat(row.getProduct()).isEqualTo("Wheat Flour");
        assertThat(row.getQty()).isEqualTo(15);
        assertThat(row.getUnit()).isEqualTo("kg");
        assertThat(row.getCostPerUnit()).isEqualByComparingTo("0.80");
        assertThat(row.getTotalCost()).isEqualByComparingTo("12.00");
        assertThat(row.getRef()).isEqualTo("PO-01");
    }

    @Test
    void wastageReport_includesDayEndClosingAndDayProductionWastage() {
        // Mock Outlet
        Outlet outlet = Outlet.builder().outletId(5L).name("South POS").build();
        when(outletRepository.findAll()).thenReturn(List.of(outlet));

        // DayEndClosing
        DayEndClosing dec = DayEndClosing.builder()
                .id(10)
                .outletId(5L)
                .closingDate(LocalDate.of(2026, 7, 4))
                .build();
        Product productA = Product.builder()
                .id(100L)
                .productName("Bun A")
                .unitPrice(1.00)
                .build();
        DayEndClosingItem decItem = DayEndClosingItem.builder()
                .id(1)
                .dayEndClosing(dec)
                .product(productA)
                .systemQty(10)
                .physicalQty(6)
                .wastageQty(4)
                .build();

        // DayProduction
        DayProduction dp = DayProduction.builder()
                .productionId(20)
                .outletId(5L)
                .orderedDate(LocalDate.of(2026, 7, 4))
                .isActive(true)
                .build();
        Product productB = Product.builder()
                .id(200L)
                .productName("Bun B")
                .unitPrice(2.00)
                .build();
        DayProductionItem dpItem = DayProductionItem.builder()
                .dayProductionItemId(2)
                .dayProduction(dp)
                .product(productB)
                .orderedQty(20)
                .receivedQty(18)
                .wastageQty(2)
                .build();

        when(dayEndClosingRepository.findAll()).thenReturn(List.of(dec));
        when(dayEndClosingItemRepository.findByDayEndClosing_Id(10)).thenReturn(List.of(decItem));
        when(dayProductionRepository.findAll()).thenReturn(List.of(dp));
        when(dayProductionItemRepository.findByProductionIdWithProduct(20)).thenReturn(List.of(dpItem));

        ReportEnvelopeDto env = service.getWastageReport("byProduct", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), null);

        assertThat(env.getRows()).hasSize(2);

        ReportRowDto rowDec = env.getRows().stream().filter(r -> r.getId().equals("W-DEC-1")).findFirst().orElseThrow();
        assertThat(rowDec.getProduct()).isEqualTo("Bun A");
        assertThat(rowDec.getQty()).isEqualTo(4);
        assertThat(rowDec.getTotalCost()).isEqualByComparingTo("4.00");
        assertThat(rowDec.getOutlet()).isEqualTo("South POS");
        assertThat(rowDec.getReason()).isEqualTo("Day End Closing Wastage");

        ReportRowDto rowDp = env.getRows().stream().filter(r -> r.getId().equals("W-DP-2")).findFirst().orElseThrow();
        assertThat(rowDp.getProduct()).isEqualTo("Bun B");
        assertThat(rowDp.getQty()).isEqualTo(2);
        assertThat(rowDp.getTotalCost()).isEqualByComparingTo("4.00");
        assertThat(rowDec.getOutlet()).isEqualTo("South POS");
        assertThat(rowDp.getReason()).isEqualTo("Receive/Transfer Wastage");
    }

    @Test
    void debugDb() throws Exception {
        java.sql.Connection conn = java.sql.DriverManager.getConnection(
            "jdbc:mysql://159.198.36.142:3306/bmsdb_testing?useSSL=false", "rover", "rover");
        java.sql.Statement stmt = conn.createStatement();
        java.sql.ResultSet rs = stmt.executeQuery("SELECT id, product_name, category FROM products");
        System.out.println("--- PRODUCTS ---");
        while (rs.next()) {
            System.out.println(rs.getLong("id") + ": " + rs.getString("product_name") + " (" + rs.getString("category") + ")");
        }
        System.out.println("--- SALES ---");
        rs = stmt.executeQuery("SELECT s.sale_id, s.sale_date, si.product_id, p.product_name, si.qty " +
            "FROM sales s JOIN sale_items si ON s.sale_id = si.sale_id " +
            "LEFT JOIN products p ON si.product_id = p.id");
        while (rs.next()) {
            System.out.println(rs.getInt("sale_id") + " | " + rs.getDate("sale_date") + " | Product ID: " + rs.getLong("product_id") + " | Name: " + rs.getString("product_name") + " | Qty: " + rs.getInt("qty"));
        }
        conn.close();
    }
}
